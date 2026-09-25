@file:OptIn(ExperimentalTime::class)

package lld_self.cache.mvcc_cache

import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


interface Clock {
    fun now(): Instant
}

class SystemClock : Clock {
    override fun now(): Instant = kotlin.time.Clock.System.now()
}


/**
 * One immutable version of one key, newest first in a `prev` chain.
 *
 * Visibility to a transaction is two independent predicates, one logical and one
 * physical, and TTL is the physical half:
 *
 *     commitTs <= snapshot.startTs        committed before I began
 *     deadline  > snapshot.startTime      not expired as of my snapshot
 *
 * That is the payoff of MVCC here: TTL stops being a mutation the sweeper performs
 * and becomes a read-time predicate, exactly like commitTs. Nothing has to delete
 * an expired entry for it to read as absent.
 *
 * [value] of null is a tombstone, so null values are not storable. Wrap them if
 * you need them.
 *
 * [prev] is var only so the collector can truncate a chain. Readers never follow a
 * link the collector can cut: it only cuts below the watermark, and every live
 * snapshot stops at or above the watermark.
 */
class Version<V>(
    val value: V?,
    val commitTs: Long,
    val deadline: Instant?,
    @Volatile var prev: Version<V>?
) {
    val isTombstone: Boolean get() = value == null

    fun isExpiredAt(time: Instant): Boolean = deadline != null && deadline <= time
}


/** A buffered mutation. [Put] holds a relative ttl; the deadline is set at commit. */
sealed interface Write<out V> {
    data class Put<out V>(val value: V, val ttl: Duration) : Write<V>
    data object Delete : Write<Nothing>
}


class WriteConflictException(val key: Any?, val kind: Kind) :
    RuntimeException("$kind conflict on key $key") {

    enum class Kind {
        /** Someone else committed a write to a key I also wrote. First committer wins. */
        WRITE_WRITE,

        /** Someone else committed a write to a key I read. Only checked in serializable mode. */
        READ_WRITE
    }
}
