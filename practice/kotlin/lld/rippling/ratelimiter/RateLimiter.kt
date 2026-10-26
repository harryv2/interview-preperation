package lld.rippling.ratelimiter

interface RateLimiter {
    fun allow(userId: String, nowMillis: Long): Boolean
}

class SlidingLogRateLimiter(
    private val limit: Int,
    private val windowMillis: Long,
) : RateLimiter {
    private val logs = HashMap<String, ArrayDeque<Long>>()

    @Synchronized
    override fun allow(userId: String, nowMillis: Long): Boolean {
        val log = logs.getOrPut(userId) { ArrayDeque() }
        while (log.isNotEmpty() && log.first() <= nowMillis - windowMillis) {
            log.removeFirst()
        }
        if (log.size >= limit) {
            return false
        }
        log.addLast(nowMillis)
        return true
    }
}

class BucketedRateLimiter(
    private val limit: Int,
    private val windowMillis: Long,
    private val buckets: Int = 10,
) : RateLimiter {
    private class Window(buckets: Int) {
        val starts = LongArray(buckets) { Long.MIN_VALUE }
        val counts = IntArray(buckets)
    }

    private val bucketMillis = windowMillis / buckets
    private val windows = HashMap<String, Window>()

    init {
        require(windowMillis % buckets == 0L) { "Window must split evenly into buckets" }
    }

    @Synchronized
    override fun allow(userId: String, nowMillis: Long): Boolean {
        val window = windows.getOrPut(userId) { Window(buckets) }
        val bucketStart = nowMillis / bucketMillis * bucketMillis
        val index = ((nowMillis / bucketMillis) % buckets).toInt()
        if (window.starts[index] != bucketStart) {
            window.starts[index] = bucketStart
            window.counts[index] = 0
        }

        var used = 0
        for (i in 0 until buckets) {
            if (window.starts[i] > nowMillis - windowMillis) {
                used += window.counts[i]
            }
        }
        if (used >= limit) {
            return false
        }
        window.counts[index]++
        return true
    }

    @Synchronized
    fun evictIdle(nowMillis: Long) {
        windows.entries.removeIf { (_, window) ->
            window.starts.all { it <= nowMillis - windowMillis }
        }
    }
}
