package lld.rippling.accessmanagement

import lld.rippling.accessmanagement.entity.AuditEvent
import lld.rippling.accessmanagement.entity.AuditType
import java.time.Instant

class AuditLog {
    private val events = ArrayList<AuditEvent>()

    fun record(event: AuditEvent) {
        events += event
    }

    fun all(): List<AuditEvent> {
        return events.toList()
    }

    fun forSubject(subject: String): List<AuditEvent> {
        return events.filter { it.subject == subject }
    }

    fun byActor(actor: String): List<AuditEvent> {
        return events.filter { it.actor == actor }
    }

    fun between(from: Instant, to: Instant, type: AuditType? = null): List<AuditEvent> {
        return events.filter { !it.at.isBefore(from) && it.at.isBefore(to) && (type == null || it.type == type) }
    }
}
