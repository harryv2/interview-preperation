@file:OptIn(ExperimentalTime::class)

package lld_self.cache.lru_ttl

import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class FakeClock(var current: Instant = Instant.fromEpochSeconds(0)) : Clock {
    override fun now(): Instant = current

    fun advance(seconds: Int) {
        current += seconds.seconds
    }
}

fun main() {
    val clock = FakeClock()

    LRUCacheTTL<String, String>(maxSize = 2, clock = clock).use { cache ->
        cache.add("a", "1", ttl = 30.seconds)
        check(cache.get("a") == "1")

        clock.advance(29)
        check(cache.get("a") == "1")

        clock.advance(1)
        check(cache.get("a") == null)
        check(!cache.contains("a"))
        check(cache.size == 0)
        println("expiry on get ok")

        cache.add("a", "1", ttl = 60.seconds)
        cache.add("b", "2", ttl = 60.seconds)
        check(cache.get("a") == "1")
        cache.add("c", "3", ttl = 60.seconds)
        check(cache.get("b") == null)
        check(cache.get("a") == "1" && cache.get("c") == "3")
        check(cache.size == 2)
        println("lru eviction ok")

        cache.add("a", "11", ttl = 5.seconds)
        clock.advance(4)
        check(cache.get("a") == "11")
        clock.advance(1)
        check(cache.get("a") == null)
        println("ttl reset on update ok")
    }

    println("all checks passed")
}
