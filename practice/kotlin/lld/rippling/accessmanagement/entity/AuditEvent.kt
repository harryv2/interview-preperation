package lld.rippling.accessmanagement.entity

import java.time.Instant

enum class AuditType {
    ROLE_DEFINED,
    ROLE_GRANTED,
    ROLE_REVOKED,
    ROLE_EXPIRED,
    GROUP_ROLE_GRANTED,
    GROUP_JOINED,
    GROUP_LEFT,
    GROUP_NESTED,
    GROUP_UNNESTED,
}

data class AuditEvent(
    val at: Instant,
    val actor: String,
    val type: AuditType,
    val subject: String,
    val detail: String,
)
