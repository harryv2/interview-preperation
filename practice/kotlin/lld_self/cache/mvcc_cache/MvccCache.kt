@file:OptIn(ExperimentalTime::class)

package lld_self.cache.mvcc_cache

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


/**
 * A transactional in-memory store with TTL, versioned rather than overwritten.
 *
 * There is no LRU here on purpose. MVCC requires a version to stay readable until
 * every transaction that could need it has finished; LRU evicts on recency, which
 * is a different ordering, so it can drop a version a live snapshot is entitled to
 * read, and "evicted" is indistinguishable from "never written". Once eviction has
 * to consult the watermark to know what is safe to drop, it is not LRU, it is [gc].
 *
 * The bound on memory is therefore the collector, not a maxSize. That is the trade:
 * a policy that could not be wrong has been replaced by one that can. An abandoned
 * transaction pins the watermark and leaks every version behind it.
 *
 * Locking: reads walk immutable [Version] objects and take no lock. Only [begin],
 * commit and [gc] serialize, and each holds the lock for a handful of instructions.
 */
class MvccCache<K, V>(
    val clock: Clock = SystemClock(),
    gcIntervalSeconds: Long = 10
) : AutoCloseable {

    private class Snapshot(val startTs: Long, val startTime: Instant)

    private val store = ConcurrentHashMap<K, AtomicReference<Version<V>>>()
    private val active = ConcurrentHashMap<Long, Snapshot>()

    private val commitTsCounter = AtomicLong(0)
    private val txnIdCounter = AtomicLong(0)

    /** Guards snapshot registration, commit and collection against each other. */
    private val txnLock = ReentrantLock()

    private val collector = if (gcIntervalSeconds > 0) {
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "mvcc-gc").apply { isDaemon = true }
        }.also { it.scheduleAtFixedRate({ gc() }, gcIntervalSeconds, gcIntervalSeconds, TimeUnit.SECONDS) }
    } else null

    // ---- transactions ----

    /**
     * Takes a snapshot. Registration and timestamp are taken under the same lock as
     * commit, so a snapshot can never be published after a collection that did not
     * account for it.
     *
     * [serializable] additionally validates the read set at commit. Without it you
     * get snapshot isolation, which permits write skew.
     */
    fun begin(serializable: Boolean = false): MvccTransaction<K, V> = txnLock.withLock {
        val id = txnIdCounter.incrementAndGet()
        val snapshot = Snapshot(commitTsCounter.get(), clock.now())
        active[id] = snapshot
        MvccTransaction(this, id, snapshot.startTs, snapshot.startTime, serializable)
    }

    /** Runs [block] in a transaction, retrying it from a fresh snapshot on conflict. */
    fun <R> transact(
        retries: Int = 1000,
        serializable: Boolean = false,
        block: (MvccTransaction<K, V>) -> R
    ): R {
        var attempts = 0
        while (true) {
            val txn = begin(serializable)
            try {
                val result = block(txn)
                txn.commit()
                return result
            } catch (e: WriteConflictException) {
                txn.rollbackQuietly()
                retryCount.incrementAndGet()
                if (++attempts > retries) throw e
            } catch (e: Throwable) {
                txn.rollbackQuietly()
                throw e
            }
        }
    }

    // ---- single-statement convenience, each its own transaction ----

    fun get(key: K): V? = transact { it.get(key) }

    fun put(key: K, value: V, ttl: Duration) = transact { it.put(key, value, ttl) }

    fun delete(key: K) = transact { it.delete(key) }

    // ---- called by MvccTransaction ----

    internal fun readAtSnapshot(key: K, startTs: Long, startTime: Instant): V? {
        var v = store[key]?.get()
        while (v != null && v.commitTs > startTs) v = v.prev   // rewind past newer commits
        if (v == null || v.isTombstone) return null            // never existed, or deleted
        if (v.isExpiredAt(startTime)) return null              // expired as of my snapshot
        return v.value
    }

    internal fun commitInternal(startTs: Long, writes: Map<K, Write<V>>, reads: Set<K>) {
        if (writes.isEmpty()) return       // read-only transactions can never conflict
        txnLock.withLock {
            for (key in writes.keys) validate(key, startTs, WriteConflictException.Kind.WRITE_WRITE)
            for (key in reads) {
                if (key !in writes) validate(key, startTs, WriteConflictException.Kind.READ_WRITE)
            }

            val commitTs = commitTsCounter.incrementAndGet()
            val now = clock.now()
            for ((key, write) in writes) {
                val newVersion: (Version<V>?) -> Version<V> = { prev ->
                    when (write) {
                        is Write.Put -> Version(write.value, commitTs, now + write.ttl, prev)
                        Write.Delete -> Version(null, commitTs, null, prev)
                    }
                }
                val ref = store[key]
                if (ref == null) store[key] = AtomicReference(newVersion(null))
                else ref.set(newVersion(ref.get()))
            }
        }
    }

    internal fun release(txnId: Long) {
        active.remove(txnId)
    }

    private fun validate(key: K, startTs: Long, kind: WriteConflictException.Kind) {
        val head = store[key]?.get() ?: return
        if (head.commitTs > startTs) throw WriteConflictException(key, kind)
    }

    // ---- collection ----

    /**
     * Reclaims versions no live or future snapshot can reach. Returns how many.
     *
     * Two rules, both keyed off the watermark (the oldest live snapshot, or now if
     * there are none):
     *
     *  1. Truncate: keep the newest version at or below the watermark, cut the rest.
     *     Safe because every live snapshot stops at or above that version.
     *  2. Drop the key entirely when its head is at or below the watermark AND is a
     *     tombstone or expired. Being at or below the watermark means every live
     *     snapshot resolves to the head, so if the head shows nothing, nobody can
     *     see anything under this key. This is how TTL actually frees memory.
     */
    fun gc(): Int = txnLock.withLock {
        var tsMark = commitTsCounter.get()
        var timeMark = clock.now()
        for (snapshot in active.values) {
            if (snapshot.startTs < tsMark) tsMark = snapshot.startTs
            if (snapshot.startTime < timeMark) timeMark = snapshot.startTime
        }

        var reclaimed = 0
        val deadKeys = ArrayList<K>()

        for ((key, ref) in store) {
            val head = ref.get()

            var oldestVisible: Version<V>? = head
            while (oldestVisible != null && oldestVisible.commitTs > tsMark) {
                oldestVisible = oldestVisible.prev
            }
            if (oldestVisible?.prev != null) {
                reclaimed += chainLength(oldestVisible.prev)
                oldestVisible.prev = null
            }

            val headInvisibleToAll = head.commitTs <= tsMark &&
                (head.isTombstone || head.isExpiredAt(timeMark))
            if (headInvisibleToAll && head.prev == null) {
                deadKeys += key
                reclaimed++
            }
        }

        deadKeys.forEach { store.remove(it) }
        reclaimed
    }

    // ---- observability, for the demo ----

    val keyCount: Int get() = store.size

    val versionCount: Int get() = store.values.sumOf { chainLength(it.get()) }

    val activeTransactions: Int get() = active.size

    internal val retryCount = AtomicLong(0)

    val conflictRetries: Long get() = retryCount.get()

    fun chainLengthOf(key: K): Int = chainLength(store[key]?.get())

    private fun chainLength(from: Version<V>?): Int {
        var n = 0
        var v = from
        while (v != null) { n++; v = v.prev }
        return n
    }

    override fun close() {
        collector?.shutdownNow()
    }
}
