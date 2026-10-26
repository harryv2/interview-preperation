package lld.rippling.expenserules.entity

enum class Severity { WARN, BLOCK }

data class Violation(
    val ruleName: String,
    val targetId: String,
    val message: String,
    val severity: Severity,
)
