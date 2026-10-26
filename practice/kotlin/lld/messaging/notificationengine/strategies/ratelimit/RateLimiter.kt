package lld.messaging.notificationengine.strategies.ratelimit

interface RateLimiter {
    fun tryAcquire(key: String): Boolean

    fun release(key: String)
}
