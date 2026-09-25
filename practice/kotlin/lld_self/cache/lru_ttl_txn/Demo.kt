@file:OptIn(ExperimentalTime::class)

package lld_self.cache.lru_ttl_txn

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
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
    cacheChecks()
    flatTransactionChecks()
    transactionChecks()
    nestedChecks()
    writeOrderChecks()
    atomicVisibilityCheck()
    println("all checks passed")
}

private fun cacheChecks() {
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

        cache.add("z", "9", ttl = 5.seconds)
        check(cache.contains("z"))
        clock.advance(5)
        check(!cache.contains("z"))
        println("contains respects expiry ok")
    }
}

private fun flatTransactionChecks() {
    val clock = FakeClock()

    LRUCacheTTL<String, String>(maxSize = 4, clock = clock).use { cache ->

        val t1 = FlatCacheTransaction(cache)
        t1.put("a", "1", 60.seconds)
        check(t1.get("a") == "1")
        check(cache.get("a") == null)
        t1.commit()
        check(cache.get("a") == "1")
        println("[flat] read-your-writes + atomic visibility ok")

        val t2 = FlatCacheTransaction(cache)
        t2.put("b", "2", 60.seconds)
        t2.delete("a")
        check(t2.get("a") == null)
        check(t2.get("b") == "2")
        t2.rollback()
        check(cache.get("a") == "1" && cache.get("b") == null)
        println("[flat] tombstone read + rollback ok")

        val t3 = FlatCacheTransaction(cache)
        t3.delete("a")
        t3.put("c", "first", 60.seconds)
        t3.delete("c")
        t3.put("c", "last", 60.seconds)
        t3.commit()
        check(cache.get("a") == null && cache.get("c") == "last")
        println("[flat] committed delete + collapsed writes ok")

        val t4 = FlatCacheTransaction(cache)
        t4.put("d", "4", 10.seconds)
        clock.advance(8)
        t4.commit()
        clock.advance(9)
        check(cache.get("d") == "4")
        clock.advance(1)
        check(cache.get("d") == null)
        println("[flat] ttl starts at commit ok")

        val t5 = FlatCacheTransaction(cache)
        t5.commit()
        check(runCatching { t5.put("e", "5", 60.seconds) }.isFailure)
        check(runCatching { t5.rollback() }.isFailure)
        println("[flat] finished transaction rejects everything ok")

        val returned = cache.runInFlatTransaction { txn ->
            txn.put("f", "6", 60.seconds)
            txn.get("f")!!.uppercase()
        }
        check(returned == "6" && cache.get("f") == "6")

        runCatching {
            cache.runInFlatTransaction { txn ->
                txn.put("g", "7", 60.seconds)
                error("boom")
            }
        }.let { check(it.isFailure) }
        check(cache.get("g") == null)
        println("[flat] runInFlatTransaction commit + auto-rollback ok")
    }
}

private fun transactionChecks() {
    val clock = FakeClock()

    LRUCacheTTL<String, String>(maxSize = 4, clock = clock).use { cache ->

        cache.runInTransaction { txn ->
            txn.put("a", "1", 60.seconds)
            check(txn.get("a") == "1")
            check(cache.get("a") == null)
        }
        check(cache.get("a") == "1")
        println("read-your-writes + atomic visibility ok")

        runCatching {
            cache.runInTransaction { txn ->
                txn.put("b", "2", 60.seconds)
                txn.delete("a")
                check(txn.get("a") == null)
                check(txn.get("b") == "2")
                error("boom")
            }
        }.let { check(it.isFailure) }
        check(cache.get("a") == "1" && cache.get("b") == null)
        println("tombstone read + auto-rollback on throw ok")

        cache.runInTransaction { it.delete("a") }
        check(cache.get("a") == null)
        println("committed delete ok")

        cache.runInTransaction { txn ->
            txn.put("c", "first", 60.seconds)
            txn.delete("c")
            txn.put("c", "last", 60.seconds)
        }
        check(cache.get("c") == "last")
        println("buffer collapses repeated writes ok")

        cache.runInTransaction { txn ->
            txn.put("d", "4", 10.seconds)
            clock.advance(8)
        }
        clock.advance(9)
        check(cache.get("d") == "4")
        clock.advance(1)
        check(cache.get("d") == null)
        println("ttl starts at commit ok")

        val returned = cache.runInTransaction { txn ->
            txn.put("f", "6", 60.seconds)
            txn.get("f")!!.uppercase()
        }
        check(returned == "6" && cache.get("f") == "6")
        println("runInTransaction returns the block value ok")

        var escaped: CacheTransaction<String, String>? = null
        cache.runInTransaction { txn -> escaped = txn }
        check(!escaped!!.isOpen)
        check(runCatching { escaped!!.put("g", "7", 60.seconds) }.isFailure)
        check(runCatching { escaped!!.get("g") }.isFailure)
        println("escaped transaction rejects everything ok")

        cache.runInTransaction { txn ->
            listOf("k1", "k2", "k3", "k4", "k5", "k6").forEach { txn.put(it, it, 60.seconds) }
        }
        check(cache.size == 4)
        check(cache.get("k1") == null && cache.get("k2") == null)
        check(cache.get("k6") == "k6")
        println("eviction during commit ok")
    }
}

private fun nestedChecks() {
    val clock = FakeClock()

    LRUCacheTTL<String, String>(maxSize = 4, clock = clock).use { cache ->

        cache.runInTransaction { txn ->
            txn.put("x", "outer", 60.seconds)
            runCatching {
                txn.nested { inner ->
                    check(txn.depth == 2)
                    inner.put("x", "inner", 60.seconds)
                    check(inner.get("x") == "inner")
                    error("boom")
                }
            }.let { check(it.isFailure) }
            check(txn.depth == 1)
            check(txn.get("x") == "outer")
        }
        check(cache.get("x") == "outer")
        println("nested discard on throw ok")

        cache.runInTransaction { txn ->
            txn.put("y", "outer", 60.seconds)
            val got = txn.nested { inner ->
                inner.put("y", "inner", 60.seconds)
                inner.get("y")
            }
            check(got == "inner")
            check(txn.get("y") == "inner")
            check(cache.get("y") == null)
        }
        check(cache.get("y") == "inner")
        println("nested merges down, cache untouched until the end ok")

        cache.runInTransaction { txn ->
            txn.put("z", "keep", 60.seconds)
            val kept = txn.nestedOrDiscard { inner ->
                inner.put("z", "maybe", 60.seconds)
                false
            }
            check(!kept)
            check(txn.get("z") == "keep")

            val kept2 = txn.nestedOrDiscard { inner ->
                inner.put("z2", "yes", 60.seconds)
                true
            }
            check(kept2)
            check(txn.get("z2") == "yes")
        }
        check(cache.get("z") == "keep" && cache.get("z2") == "yes")
        println("nestedOrDiscard both ways ok")

        cache.runInTransaction { txn ->
            txn.put("w", "l1", 60.seconds)
            txn.nested { l2 ->
                l2.put("w", "l2", 60.seconds)
                runCatching {
                    l2.nested { l3 ->
                        check(txn.depth == 3)
                        l3.delete("w")
                        check(l3.get("w") == null)
                        error("boom")
                    }
                }
                check(txn.depth == 2)
                check(l2.get("w") == "l2")
            }
            check(txn.depth == 1)
            check(txn.get("w") == "l2")
        }
        check(cache.get("w") == "l2")
        println("deep nesting unwinds one frame at a time ok")

        cache.runInTransaction { txn ->
            repeat(5) { i -> txn.nested { it.put("leak$i", "$i", 60.seconds) } }
            check(txn.depth == 1)
        }
        check(cache.size == 4)
        println("no frame can leak ok")
    }
}

private fun writeOrderChecks() {
    LRUCacheTTL<String, String>(maxSize = 2, clock = FakeClock()).use { cache ->
        val txn = FlatCacheTransaction(cache)
        txn.put("a", "1", 60.seconds)
        txn.put("b", "2", 60.seconds)
        txn.put("a", "1-updated", 60.seconds)
        txn.put("c", "3", 60.seconds)
        txn.commit()

        check(cache.get("a") == "1-updated")
        check(cache.get("b") == null)
        check(cache.get("c") == "3")
        println("[flat] rewrite moves the key to most recent ok")
    }

    LRUCacheTTL<String, String>(maxSize = 2, clock = FakeClock()).use { cache ->
        cache.runInTransaction { txn ->
            txn.put("a", "1", 60.seconds)
            txn.put("b", "2", 60.seconds)
            txn.nested { inner -> inner.put("a", "1-updated", 60.seconds) }
            txn.put("c", "3", 60.seconds)
        }

        check(cache.get("a") == "1-updated")
        check(cache.get("b") == null)
        check(cache.get("c") == "3")
        println("nested merge keeps the child's position ok")
    }

    val script: List<Pair<String, String?>> = listOf(
        "a" to "1", "b" to "2", "a" to "1b", "c" to "3",
        "b" to null, "d" to "4", "a" to "1c"
    )

    LRUCacheTTL<String, String>(maxSize = 2, clock = FakeClock()).use { sequential ->
        LRUCacheTTL<String, String>(maxSize = 2, clock = FakeClock()).use { transactional ->
            script.forEach { (key, value) ->
                if (value == null) sequential.delete(key)
                else sequential.add(key, value, 60.seconds)
            }

            transactional.runInTransaction { txn ->
                script.forEach { (key, value) ->
                    if (value == null) txn.delete(key)
                    else txn.put(key, value, 60.seconds)
                }
            }

            for (key in listOf("a", "b", "c", "d")) {
                check(sequential.get(key) == transactional.get(key)) {
                    "key $key: sequential=${sequential.get(key)}, transaction=${transactional.get(key)}"
                }
            }
            println("commit matches sequential writes ok")
        }
    }
}

private fun atomicVisibilityCheck() {
    LRUCacheTTL<String, String>(maxSize = 4, clock = SystemClock()).use { cache ->
        val stop = AtomicBoolean(false)
        val halfSeen = AtomicInteger(0)
        val samples = AtomicInteger(0)
        val keys = listOf("p", "q")

        val reader = thread(name = "reader") {
            while (!stop.get()) {
                val seen = cache.getAll(keys)
                if (seen.size == 1) halfSeen.incrementAndGet()
                samples.incrementAndGet()
            }
        }

        repeat(20_000) {
            cache.runInTransaction { txn ->
                txn.put("p", "1", 60.seconds)
                txn.put("q", "2", 60.seconds)
            }
            cache.runInTransaction { txn ->
                txn.delete("p")
                txn.delete("q")
            }
        }

        stop.set(true)
        reader.join()

        check(samples.get() > 0) { "reader never ran" }
        check(halfSeen.get() == 0) { "saw ${halfSeen.get()} half-applied transactions" }
        println("no half-applied transaction observed in ${samples.get()} samples")
    }
}
