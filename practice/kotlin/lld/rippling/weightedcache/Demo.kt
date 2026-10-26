package lld.rippling.weightedcache

fun main() {
    WeightedTtlCache<String, String>(maxWeight = 10, sweepEveryMillis = 50).use { cache ->
        var quiet = false
        cache.onEvict { key, reason ->
            if (!quiet) {
                println("  evicted $key ($reason)")
            }
        }

        println("-- fill to the weight limit")
        cache.put("a", "A", weight = 4, ttlMillis = 10_000)
        cache.put("b", "B", weight = 4, ttlMillis = 10_000)
        cache.get("a")
        println("  weight=${cache.weight}")

        println("-- going over the limit evicts least recently used in the background")
        cache.put("c", "C", weight = 5, ttlMillis = 10_000)
        Thread.sleep(30)
        println("  a=${cache.get("a")} b=${cache.get("b")} c=${cache.get("c")} weight=${cache.weight}")

        println("-- short ttl expires, get is lazy, sweeper is periodic")
        cache.put("d", "D", weight = 1, ttlMillis = 40)
        println("  d=${cache.get("d")}")
        Thread.sleep(120)
        println("  size=${cache.size} weight=${cache.weight}")

        println("-- many threads writing at once")
        val threads = (1..8).map { t ->
            Thread {
                repeat(200) { i ->
                    cache.put("t$t-$i", "v", weight = 1, ttlMillis = 10_000)
                    cache.get("t$t-${i / 2}")
                }
            }
        }
        quiet = true
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        cache.cleanup()
        println("  weight after cleanup=${cache.weight} (limit 10)")
    }
}
