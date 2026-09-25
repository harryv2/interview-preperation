package lld.messaging.notificationengine.strategies.ratelimit

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

// approximate: counts the previous fixed window weighted by how much of it still overlaps
class SlidingCounterRateLimiter(
    private val limit: Int,
    window: Duration,
) : RateLimiter {
    private val windowNanos = window.inWholeNanoseconds
    private val lock = ReentrantLock()
    private var windowStart = System.nanoTime()
    private var current = 0
    private var previous = 0

    override fun tryAcquire(key: String): Boolean {
        lock.withLock {
            val now = System.nanoTime()
            rollWindow(now)

            val elapsedFraction = (now - windowStart).toDouble() / windowNanos
            val estimate = current + previous * (1 - elapsedFraction)
            if (estimate >= limit) return false

            current++
            return true
        }
    }

    private fun rollWindow(now: Long) {
        val windowsPassed = (now - windowStart) / windowNanos
        if (windowsPassed == 0L) return

        previous = if (windowsPassed == 1L) current else 0
        current = 0
        windowStart += windowsPassed * windowNanos
    }
}
