package lld.notificationengine.strategies.ratelimit

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min

class TokenBucketRateLimiter(
    private val capacity: Int,
    private val refillPerSecond: Double,
) : RateLimiter {
    private val lock = ReentrantLock()
    private var tokens = capacity.toDouble()
    private var lastRefillNanos = System.nanoTime()

    override fun tryAcquire(key: String): Boolean {
        lock.withLock {
            refill()
            if (tokens < 1) return false
            tokens -= 1
            return true
        }
    }

    private fun refill() {
        val now = System.nanoTime()
        val elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0
        tokens = min(capacity.toDouble(), tokens + elapsedSeconds * refillPerSecond)
        lastRefillNanos = now
    }
}
