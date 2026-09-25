package lld.messaging.notificationengine.strategies.ratelimit

class CompositeRateLimiter(private val limiters: List<RateLimiter>) : RateLimiter {

    // order matters on refusal: a wasted burst token refills, a wasted daily slot does not,
    // so burst first, per-user next, channel daily last
    override fun tryAcquire(key: String): Boolean {
        return limiters.all { it.tryAcquire(key) }
    }
}
