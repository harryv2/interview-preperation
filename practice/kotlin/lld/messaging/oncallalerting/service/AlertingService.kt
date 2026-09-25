package lld.messaging.oncallalerting.service

import lld.messaging.oncallalerting.entity.ChannelType
import lld.messaging.oncallalerting.entity.Incident
import lld.messaging.oncallalerting.entity.IncidentStatus
import lld.messaging.oncallalerting.entity.Project
import lld.messaging.oncallalerting.entity.Responder
import lld.messaging.oncallalerting.entity.Severity
import lld.messaging.oncallalerting.strategy.AlertChannel
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


class AlertingService(channels: List<AlertChannel>) {

    private val channels: Map<ChannelType, AlertChannel> = channels.associateBy { it.type }
    private val projects = ConcurrentHashMap<String, Project>()
    private val incidents = ConcurrentHashMap<String, Incident>()

    // one pending timer per incident, so acknowledging can cancel the page that was going to wake the next
    // person up
    private val timers = ConcurrentHashMap<String, ScheduledFuture<*>>()

    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "escalation-timer").apply { isDaemon = true }
    }

    fun register(project: Project) {
        val clash = projects.putIfAbsent(project.id, project)
        require(clash == null) { "Project ${project.id} is already registered" }
    }

    fun raise(projectId: String, title: String, severity: Severity): Incident {
        val project = requireNotNull(projects[projectId]) { "No project $projectId" }

        val incident = Incident("INC-${UUID.randomUUID().toString().take(6)}", project, title, severity)
        incidents[incident.id] = incident
        incident.record("triggered at level 0")

        page(incident)
        return incident
    }

    fun acknowledge(incidentId: String, responder: Responder): Boolean {
        val incident = incident(incidentId)
        if (!incident.acknowledge(responder)) return false

        stopTimer(incident)
        return true
    }

    fun resolve(incidentId: String, responder: Responder): Boolean {
        val incident = incident(incidentId)
        if (!incident.resolve(responder)) return false

        stopTimer(incident)
        return true
    }

    fun incident(incidentId: String): Incident {
        return requireNotNull(incidents[incidentId]) { "No incident $incidentId" }
    }

    fun open(): List<Incident> {
        return incidents.values.filter { it.status != IncidentStatus.RESOLVED }
    }

    // Wakes the current level and arms the timer that moves to the next one. Both halves have to happen for
    // an unacknowledged page to keep travelling, so they live in one place rather than at every call site.
    private fun page(incident: Incident) {
        val level = incident.project.policy.levelAt(incident.level)

        level.responders.forEach { responder ->
            level.channels.forEach { type ->
                val channel = channels[type]
                if (channel == null) {
                    incident.record("no sender wired for $type")
                    return@forEach
                }

                // one dead carrier must not swallow the rest of the fan out, a page that half arrives still
                // wakes somebody up
                runCatching { channel.page(responder, incident) }
                    .onFailure { incident.record("$type to ${responder.name} failed: ${it.message}") }
            }
        }

        armTimer(incident)
    }

    private fun armTimer(incident: Incident) {
        if (incident.level == incident.project.policy.lastLevel) return

        val firesAt = incident.level
        val waitFor = incident.project.policy.levelAt(firesAt).waitFor

        timers[incident.id] = scheduler.schedule(
            { onTimeout(incident, firesAt) },
            waitFor.toMillis(),
            TimeUnit.MILLISECONDS
        )
    }

    private fun onTimeout(incident: Incident, firesAt: Int) {
        if (!incident.escalate(firesAt)) return

        page(incident)
    }

    // cancel(false) cannot stop a timer that is already inside onTimeout, which is why escalate() checks the
    // level it was armed for rather than trusting this to be enough
    private fun stopTimer(incident: Incident) {
        timers.remove(incident.id)?.cancel(false)
    }

    fun shutdown() {
        scheduler.shutdownNow()
    }
}
