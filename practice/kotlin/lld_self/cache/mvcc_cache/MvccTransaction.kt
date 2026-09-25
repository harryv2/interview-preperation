@file:OptIn(ExperimentalTime::class)

package lld_self.cache.mvcc_cache

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


/**
 * The same write buffer as the locking version, reading from a snapshot instead of
 * from live state.
 *
 * Only two things changed from the flat transaction over the LRU cache:
 *   - the fallback read is pinned to (startTs, startTime) instead of reading "now"
 *   - commit validates before installing, so the loser aborts instead of overwriting
 *
 * Buffer, tombstones and read-your-writes are untouched. MVCC replaces where the
 * committed state lives, not the transaction layer on top of it.
 *
 * Isolation is snapshot isolation, which permits write skew: two transactions read
 * overlapping keys, write disjoint ones, and neither trips write-write validation.
 * [serializable] closes that by validating the read set too. With point reads only
 * and no range scans, that is genuinely serializable; add range queries and you
 * would need phantom protection as well.
 */
class MvccTransaction<K, V> internal constructor(
    private val cache: MvccCache<K, V>,
    private val id: Long,
    val startTs: Long,
    val startTime: Instant,
    private val serializable: Boolean
) : AutoCloseable {

    private val buffer = LinkedHashMap<K, Write<V>>()
    private val readSet = LinkedHashSet<K>()
    private var finished = false

    val isOpen: Boolean get() = !finished

    fun get(key: K): V? {
        checkOpen()
        when (val write = buffer[key]) {
            is Write.Put -> return write.value     // read your own write
            Write.Delete -> return null            // read your own delete
            null -> Unit                           // untouched here, fall through
        }
        readSet += key
        return cache.readAtSnapshot(key, startTs, startTime)
    }

    fun contains(key: K): Boolean = get(key) != null

    fun put(key: K, value: V, ttl: Duration) {
        checkOpen()
        buffer[key] = Write.Put(value, ttl)
    }

    fun delete(key: K) {
        checkOpen()
        buffer[key] = Write.Delete
    }

    /** Throws [WriteConflictException] and ends the transaction if validation fails. */
    fun commit() {
        checkOpen()
        try {
            cache.commitInternal(startTs, buffer, if (serializable) readSet else emptySet())
        } finally {
            finish()
        }
    }

    fun rollback() {
        checkOpen()
        buffer.clear()
        finish()
    }

    fun rollbackQuietly() {
        if (isOpen) rollback()
    }

    /** Releasing the snapshot is what lets the collector move past it. */
    override fun close() = rollbackQuietly()

    private fun finish() {
        finished = true
        cache.release(id)
    }

    private fun checkOpen() = check(!finished) { "transaction already finished" }
}
