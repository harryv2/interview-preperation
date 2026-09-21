package lld.notificationengine.strategies.ratelimit

import java.util.concurrent.ConcurrentHashMap

class PerUserRateLimiter(private val newLimiter: () -> RateLimiter) : RateLimiter {
    private val limiters = ConcurrentHashMap<String, RateLimiter>()

    override fun tryAcquire(key: String): Boolean {
        return limiters.computeIfAbsent(key) { newLimiter() }.tryAcquire(key)
    }
}
