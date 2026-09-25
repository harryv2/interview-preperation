@file:OptIn(ExperimentalTime::class)

package lld_self.cache.mvcc_cache

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

private val HOUR = 3600.seconds


fun main() {
    basicChecks()
    snapshotIsolationChecks()
    ttlChecks()
    conflictChecks()
    writeSkewChecks()
    gcChecks()
    lostUpdateCheck()
    consistentMultiKeyReadCheck()
    println("all checks passed")
}


/** The transaction layer behaves exactly as it did over the locking cache. */
private fun basicChecks() {
    val clock = FakeClock()
    // gcIntervalSeconds = 0 disables the background collector so counts stay deterministic
    MvccCache<String, String>(clock, gcIntervalSeconds = 0).use { cache ->

        val t1 = cache.begin()
        t1.put("a", "1", HOUR)
        check(t1.get("a") == "1")          // read your own write
        check(cache.get("a") == null)      // invisible outside until commit
        t1.commit()
        check(cache.get("a") == "1")
        println("read-your-writes + atomic visibility ok")

        val t2 = cache.begin()
        t2.put("b", "2", HOUR)
        t2.delete("a")
        check(t2.get("a") == null)         // tombstone read
        check(t2.get("b") == "2")
        t2.rollback()
        check(cache.get("a") == "1" && cache.get("b") == null)
        println("tombstone read + rollback ok")

        cache.transact { it.delete("a") }
        check(cache.get("a") == null)
        check(cache.chainLengthOf("a") == 2)   // the tombstone sits on top of the old value
        println("committed delete leaves a tombstone ok")
    }
}


/** The thing the locking cache could not give you: a stable view across time. */
private fun snapshotIsolationChecks() {
    val clock = FakeClock()
    MvccCache<String, String>(clock, gcIntervalSeconds = 0).use { cache ->
        cache.put("a", "v1", HOUR)

        val reader = cache.begin()
        check(reader.get("a") == "v1")

        cache.put("a", "v2", HOUR)         // another transaction commits underneath it

        check(reader.get("a") == "v1")     // repeatable read: still sees its own snapshot
        check(cache.get("a") == "v2")      // a fresh snapshot sees the new value
        reader.close()

        check(cache.get("a") == "v2")
        println("repeatable read across a concurrent commit ok")

        // a snapshot also hides a key created after it began
        val r2 = cache.begin()
        cache.put("fresh", "x", HOUR)
        check(r2.get("fresh") == null)
        check(cache.get("fresh") == "x")
        r2.close()
        println("snapshot hides later inserts ok")
    }
}


/** TTL is a visibility predicate here, not a deletion. */
private fun ttlChecks() {
    val clock = FakeClock()
    MvccCache<String, String>(clock, gcIntervalSeconds = 0).use { cache ->

        cache.put("a", "1", 30.seconds)
        clock.advance(29)
        check(cache.get("a") == "1")
        clock.advance(1)
        check(cache.get("a") == null)      // expired with nothing having deleted it
        println("expiry with no sweeper ok")

        // a long-running transaction keeps seeing an entry that expired mid-flight
        cache.put("b", "2", 10.seconds)
        val long = cache.begin()
        check(long.get("b") == "2")
        clock.advance(60)
        check(long.get("b") == "2")        // still visible: expiry is judged at my snapshot
        check(cache.get("b") == null)      // a fresh snapshot sees it gone
        long.close()
        println("ttl judged at snapshot time, not read time ok")

        // the ttl clock starts at commit, not at put
        val t = cache.begin()
        t.put("c", "3", 10.seconds)
        clock.advance(8)
        t.commit()
        clock.advance(9)
        check(cache.get("c") == "3")
        clock.advance(1)
        check(cache.get("c") == null)
        println("ttl starts at commit ok")
    }
}


/** First committer wins, and the loser is told rather than silently overwritten. */
private fun conflictChecks() {
    val clock = FakeClock()
    MvccCache<String, String>(clock, gcIntervalSeconds = 0).use { cache ->
        cache.put("a", "base", HOUR)

        val t1 = cache.begin()
        val t2 = cache.begin()
        t1.put("a", "from-t1", HOUR)
        t2.put("a", "from-t2", HOUR)

        t1.commit()
        val outcome = runCatching { t2.commit() }
        check(outcome.isFailure)
        val e = outcome.exceptionOrNull() as WriteConflictException
        check(e.kind == WriteConflictException.Kind.WRITE_WRITE && e.key == "a")
        check(cache.get("a") == "from-t1")
        println("write-write conflict aborts the second committer ok")

        // disjoint writes from concurrent snapshots both succeed
        val t3 = cache.begin()
        val t4 = cache.begin()
        t3.put("x", "1", HOUR)
        t4.put("y", "2", HOUR)
        t3.commit(); t4.commit()
        check(cache.get("x") == "1" && cache.get("y") == "2")
        println("disjoint writes do not conflict ok")

        // a read-only transaction never conflicts, however long it is held
        val readOnly = cache.begin()
        check(readOnly.get("a") == "from-t1")
        cache.put("a", "later", HOUR)
        readOnly.commit()
        println("read-only transaction never conflicts ok")
    }
}


/**
 * Snapshot isolation permits write skew, and this check proves it rather than
 * asserting it. Two transactions read the same two keys, each writes only its own,
 * so write-write validation sees nothing wrong and the cross-key invariant breaks.
 */
private fun writeSkewChecks() {
    val clock = FakeClock()

    MvccCache<String, Int>(clock, gcIntervalSeconds = 0).use { cache ->
        // invariant we intend to hold: quotaA + quotaB <= 100
        cache.put("quotaA", 40, HOUR)
        cache.put("quotaB", 40, HOUR)

        val t1 = cache.begin()
        val t2 = cache.begin()

        val t1Headroom = 100 - (t1.get("quotaA")!! + t1.get("quotaB")!!)   // 20
        val t2Headroom = 100 - (t2.get("quotaA")!! + t2.get("quotaB")!!)   // 20
        check(t1Headroom == 20 && t2Headroom == 20)

        t1.put("quotaA", 40 + t1Headroom, HOUR)    // writes only quotaA
        t2.put("quotaB", 40 + t2Headroom, HOUR)    // writes only quotaB

        t1.commit()
        t2.commit()                                 // no conflict: disjoint write sets

        val total = cache.get("quotaA")!! + cache.get("quotaB")!!
        check(total == 120) { "expected the invariant to break, got $total" }
        println("write skew reproduced under snapshot isolation (total=$total)")
    }

    // serializable mode validates the read set, so the second committer aborts
    MvccCache<String, Int>(FakeClock(), gcIntervalSeconds = 0).use { cache ->
        cache.put("quotaA", 40, HOUR)
        cache.put("quotaB", 40, HOUR)

        val t1 = cache.begin(serializable = true)
        val t2 = cache.begin(serializable = true)
        t1.get("quotaA"); t1.get("quotaB")
        t2.get("quotaA"); t2.get("quotaB")

        t1.put("quotaA", 60, HOUR)
        t2.put("quotaB", 60, HOUR)

        t1.commit()
        val outcome = runCatching { t2.commit() }
        check(outcome.isFailure)
        check((outcome.exceptionOrNull() as WriteConflictException).kind
            == WriteConflictException.Kind.READ_WRITE)
        check(cache.get("quotaA")!! + cache.get("quotaB")!! == 100)
        println("serializable mode rejects the write skew ok")
    }
}


/** The collector is the memory bound now, and a live snapshot holds it back. */
private fun gcChecks() {
    val clock = FakeClock()
    MvccCache<String, String>(clock, gcIntervalSeconds = 0).use { cache ->

        cache.put("a", "v1", HOUR)
        cache.put("a", "v2", HOUR)
        cache.put("a", "v3", HOUR)
        check(cache.versionCount == 3)

        check(cache.gc() == 2)                  // nothing active, only the head survives
        check(cache.versionCount == 1 && cache.get("a") == "v3")
        println("gc truncates unreachable versions ok")

        // an open snapshot pins everything behind it
        val pinned = cache.begin()
        check(pinned.get("a") == "v3")
        cache.put("a", "v4", HOUR)
        cache.put("a", "v5", HOUR)
        check(cache.versionCount == 3)
        check(cache.gc() == 0) { "gc must not reclaim what a live snapshot can read" }
        check(pinned.get("a") == "v3")

        pinned.close()
        check(cache.gc() == 2)
        check(cache.versionCount == 1 && cache.get("a") == "v5")
        println("live snapshot holds the watermark back ok")

        // a tombstone below the watermark frees the key entirely
        cache.delete("a")
        check(cache.keyCount == 1)
        cache.gc()
        check(cache.keyCount == 0)
        println("tombstone collection frees the key ok")

        // and so does TTL, which is how expiry actually reclaims memory here
        cache.put("b", "x", 5.seconds)
        check(cache.keyCount == 1)
        clock.advance(5)
        check(cache.get("b") == null)
        cache.gc()
        check(cache.keyCount == 0)
        println("expired key collected ok")
    }
}


/**
 * The classic read-modify-write race. Under last-writer-wins these increments
 * would be lost; first-committer-wins turns each lost update into a retry.
 */
private fun lostUpdateCheck() {
    MvccCache<String, Int>(SystemClock(), gcIntervalSeconds = 0).use { cache ->
        cache.put("n", 0, HOUR)

        val threads = 4
        val perThread = 200
        val workers = (1..threads).map {
            thread {
                repeat(perThread) {
                    cache.transact { txn ->
                        txn.put("n", txn.get("n")!! + 1, HOUR)
                    }
                }
            }
        }
        workers.forEach { it.join() }

        val expected = threads * perThread
        check(cache.get("n") == expected) { "lost updates: ${cache.get("n")} != $expected" }
        println("no lost updates across $threads threads ($expected increments, ${cache.conflictRetries} retries)")
    }
}


/**
 * The locking cache needed a batched getAll() to read two keys without tearing.
 * Here two ordinary reads inside one transaction are already consistent, because
 * they resolve against the same snapshot and take no lock at all.
 */
private fun consistentMultiKeyReadCheck() {
    MvccCache<String, String>(SystemClock(), gcIntervalSeconds = 0).use { cache ->
        val stop = AtomicBoolean(false)
        val halfSeen = AtomicInteger(0)
        val samples = AtomicInteger(0)

        val reader = thread(name = "reader") {
            while (!stop.get()) {
                val seen = cache.transact { txn ->
                    listOfNotNull(txn.get("p"), txn.get("q"))   // two separate reads
                }
                if (seen.size == 1) halfSeen.incrementAndGet()
                samples.incrementAndGet()
            }
        }

        repeat(20_000) {
            cache.transact { txn ->
                txn.put("p", "1", HOUR)
                txn.put("q", "2", HOUR)
            }
            cache.transact { txn ->
                txn.delete("p")
                txn.delete("q")
            }
        }

        stop.set(true)
        reader.join()

        check(samples.get() > 0) { "reader never ran" }
        check(halfSeen.get() == 0) { "saw ${halfSeen.get()} torn reads" }
        println("no torn multi-key read in ${samples.get()} samples, with no batched read api")
    }
}
