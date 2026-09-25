package lld.messaging.oncallalerting.entity

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


enum class IncidentStatus {
    TRIGGERED,
    ACKNOWLEDGED,
    RESOLVED
}


class Incident(
    val id: String,
    val project: Project,
    val title: String,
    val severity: Severity
) {

    private val lock = ReentrantLock()
    private val timeline = mutableListOf<String>()

    var status = IncidentStatus.TRIGGERED
        private set

    var level = 0
        private set

    var acknowledgedBy: Responder? = null
        private set

    // The escalation timer for a level must not fire against an incident that has already moved on, and
    // cancelling a scheduled task cannot stop one that is already running. So the timer says which level it
    // was set for, and a level that no longer matches is a timer that lost the race.
    fun escalate(from: Int): Boolean {
        lock.withLock {
            if (status != IncidentStatus.TRIGGERED) return false
            if (level != from) return false

            if (level == project.policy.lastLevel) {
                record("nobody acknowledged, escalation exhausted at level $level")
                return false
            }

            level += 1
            record("escalated to level $level")
            return true
        }
    }

    // Not an error to be late. Two channels can deliver the same page and the responder can answer both, and
    // whoever is second is told the incident is handled rather than being handed a failure.
    fun acknowledge(responder: Responder): Boolean {
        lock.withLock {
            require(project.policy.knows(responder)) { "${responder.name} is not on call for $project" }
            if (status != IncidentStatus.TRIGGERED) return false

            status = IncidentStatus.ACKNOWLEDGED
            acknowledgedBy = responder
            record("acknowledged by ${responder.name} at level $level")
            return true
        }
    }

    fun resolve(responder: Responder): Boolean {
        lock.withLock {
            if (status == IncidentStatus.RESOLVED) return false

            status = IncidentStatus.RESOLVED
            record("resolved by ${responder.name}")
            return true
        }
    }

    fun record(event: String) {
        lock.withLock {
            timeline.add(event)
        }
    }

    fun timeline(): List<String> {
        lock.withLock {
            return timeline.toList()
        }
    }

    override fun toString(): String {
        val who = acknowledgedBy?.let { ", ack by $it" } ?: ""
        return "$id [$severity] $title on $project, level $level, $status$who"
    }
}
