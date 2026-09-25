package lld.messaging.notificationengine.strategies.ratelimit

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class SlidingWindowRateLimiter(
    private val limit: Int,
    private val window: Duration,
) : RateLimiter {
    private val lock = ReentrantLock()
    private val sentAt = ArrayDeque<Long>()

    override fun tryAcquire(key: String): Boolean {
        lock.withLock {
            val now = System.nanoTime()
            val windowStart = now - window.inWholeNanoseconds
            while (sentAt.isNotEmpty() && sentAt.first() <= windowStart) {
                sentAt.removeFirst()
            }

            if (sentAt.size >= limit) return false
            sentAt.addLast(now)
            return true
        }
    }
}
