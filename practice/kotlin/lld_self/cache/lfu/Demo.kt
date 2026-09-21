package lld_self.cache.lfu

import kotlin.random.Random

fun <T, U> LfuCache<T, U>.check(expectedOrder: List<T>) {
    val keys = keys()
    check(keys == expectedOrder) { "eviction order $keys != $expectedOrder" }
    check(size == expectedOrder.size) { "size $size != ${expectedOrder.size}" }
    check(size <= maxSize) { "size $size exceeds maxSize $maxSize" }

    val freqs = keys.map { frequency(it)!! }
    check(freqs == freqs.sorted()) { "keys $keys not ordered by frequency $freqs" }

    for (key in expectedOrder) {
        check(contains(key)) { "missing $key" }
    }
    println("ok $expectedOrder freq=$freqs")
}

fun testBasicEviction() {
    val cache = LfuCache<Int, String>(2)

    cache.add(1, "one")
    cache.add(2, "two")
    cache.check(listOf(1, 2))

    check(cache.get(1) == "one")
    cache.check(listOf(2, 1))
    check(cache.frequency(1) == 2)

    cache.add(3, "three")
    cache.check(listOf(3, 1))
    check(cache.get(2) == null)
    check(!cache.contains(2))

    check(cache.get(3) == "three")
    check(cache.get(3) == "three")
    cache.check(listOf(1, 3))

    cache.add(4, "four")
    cache.check(listOf(4, 3))
    check(cache.get(1) == null)

    check(cache.get(4) == "four")
    check(cache.get(4) == "four")
    check(cache.get(4) == "four")
    cache.check(listOf(3, 4))

    cache.add(5, "five")
    cache.check(listOf(5, 4))
    check(cache.get(3) == null)
}

fun testUpdateExistingKeyBumpsFrequency() {
    val cache = LfuCache<Int, String>(2)

    cache.add(1, "one")
    cache.add(2, "two")
    cache.add(1, "ONE")
    cache.check(listOf(2, 1))
    check(cache.frequency(1) == 2)

    cache.add(3, "three")
    cache.check(listOf(3, 1))
    check(cache.get(1) == "ONE")
    check(cache.frequency(1) == 3)
}

fun testTieBreaksByLeastRecentlyUsed() {
    val cache = LfuCache<Int, Int>(3)

    cache.add(1, 1)
    cache.add(2, 2)
    cache.add(3, 3)
    cache.get(1)
    cache.get(2)
    cache.get(3)
    cache.check(listOf(1, 2, 3))

    cache.add(4, 4)
    cache.check(listOf(4, 2, 3))

    cache.get(4)
    cache.check(listOf(2, 3, 4))

    cache.add(5, 5)
    cache.check(listOf(5, 3, 4))
}

fun testFrequencyClimbs() {
    val cache = LfuCache<String, Int>(2)

    cache.add("a", 1)
    cache.add("b", 2)
    cache.get("a")
    cache.get("b")
    cache.check(listOf("a", "b"))
    check(cache.frequency("a") == 2 && cache.frequency("b") == 2)

    cache.get("a")
    cache.get("b")
    check(cache.frequency("a") == 3 && cache.frequency("b") == 3)

    cache.add("c", 3)
    cache.check(listOf("c", "b"))
    check(cache.frequency("c") == 1)
    check(cache.frequency("a") == null)
}

fun testSingleSlot() {
    val cache = LfuCache<String, Int>(1)

    cache.add("a", 1)
    cache.add("b", 2)
    cache.check(listOf("b"))
    check(cache.get("a") == null)
    check(cache.get("b") == 2)

    cache.add("b", 3)
    cache.check(listOf("b"))
    check(cache.get("b") == 3)
    check(cache.frequency("b") == 4)
}

fun testChurn() {
    val cache = LfuCache<Int, Int>(50)

    for (i in 0 until 500) {
        cache.add(i, i * i)
        for (j in 0 until i % 7) {
            cache.get(i)
        }
    }
    cache.check(cache.keys())

    for (key in cache.keys()) {
        check(cache.get(key) == key * key)
    }
    cache.check(cache.keys())
}

fun testConcurrentAccess() {
    val cache = LfuCache<Int, Int>(100)
    val errors = java.util.concurrent.CopyOnWriteArrayList<Throwable>()

    val threads = (0 until 8).map { t ->
        Thread {
            val random = Random(t)
            try {
                repeat(20_000) {
                    val key = random.nextInt(500)
                    if (random.nextBoolean()) {
                        cache.add(key, key * 2)
                    } else {
                        val value = cache.get(key)
                        check(value == null || value == key * 2) { "bad value $value for $key" }
                    }
                }
            } catch (e: Throwable) {
                errors.add(e)
            }
        }
    }
    threads.forEach { it.start() }
    threads.forEach { it.join() }

    check(errors.isEmpty()) { "threads failed: ${errors.first()}" }
    cache.check(cache.keys())
    for (key in cache.keys()) {
        check(cache.get(key) == key * 2)
    }
    println("ok concurrent: ${cache.size} keys after ${8 * 20_000} ops")
}

fun main() {
    testBasicEviction()
    testUpdateExistingKeyBumpsFrequency()
    testTieBreaksByLeastRecentlyUsed()
    testFrequencyClimbs()
    testSingleSlot()
    testChurn()
    testConcurrentAccess()
    println("all checks passed")
}
