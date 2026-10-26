package lld.rippling.ratelimiter

fun main() {
    val limiters = listOf(
        "exact log" to SlidingLogRateLimiter(limit = 3, windowMillis = 1000),
        "10 buckets" to BucketedRateLimiter(limit = 3, windowMillis = 1000, buckets = 10),
    )

    for ((name, limiter) in limiters) {
        println("-- $name, 3 calls per second")
        val times = listOf(0L, 100, 200, 300, 999, 1000, 1150, 1250)
        val results = times.map { "$it:${limiter.allow("u1", it)}" }
        println("  $results")
        println("  other user unaffected: ${limiter.allow("u2", 300)}")
    }
}
