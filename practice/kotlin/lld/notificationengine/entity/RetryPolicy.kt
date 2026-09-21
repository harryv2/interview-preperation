package lld.notificationengine.entity

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 100,
    val multiplier: Double = 2.0,
) {
    fun delayBeforeAttempt(attempt: Int): Long {
        var delay = initialDelayMs.toDouble()
        repeat(attempt - 2) { delay *= multiplier }
        return delay.toLong()
    }
}
