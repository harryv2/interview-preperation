package lld.messaging.notificationengine.strategies.ratelimit

class CompositeRateLimiter(private val limiters: List<RateLimiter>) : RateLimiter {

    // a later refusal hands back the slots already taken, so a blocked send costs the user nothing
    override fun tryAcquire(key: String): Boolean {
        val acquired = mutableListOf<RateLimiter>()
        for (limiter in limiters) {
            if (!limiter.tryAcquire(key)) {
                acquired.asReversed().forEach { it.release(key) }
                return false
            }
            acquired.add(limiter)
        }
        return true
    }

    override fun release(key: String) {
        limiters.asReversed().forEach { it.release(key) }
    }
}
