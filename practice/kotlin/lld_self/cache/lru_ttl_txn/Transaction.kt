@file:OptIn(ExperimentalTime::class)

package lld_self.cache.lru_ttl_txn

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class CacheTransaction<K, V> internal constructor(
    private val cache: LRUCacheTTL<K, V>
) {
    private val frames = ArrayDeque<LinkedHashMap<K, Write<V>>>()

    init {
        frames.addLast(LinkedHashMap())
    }

    val isOpen: Boolean get() = frames.isNotEmpty()

    val depth: Int get() = frames.size

    fun get(key: K): V? {
        checkOpen()
        for (i in frames.indices.reversed()) {
            when (val write = frames[i][key]) {
                is Write.Put -> return write.value
                Write.Delete -> return null
                null -> Unit
            }
        }
        return cache.get(key)
    }

    fun contains(key: K): Boolean = get(key) != null

    fun put(key: K, value: V, ttl: Duration) {
        checkOpen()
        stage(key, Write.Put(value, ttl))
    }

    fun delete(key: K) {
        checkOpen()
        stage(key, Write.Delete)
    }

    fun <R> nested(block: (CacheTransaction<K, V>) -> R): R =
        runNested(block) { true }

    fun nestedOrDiscard(block: (CacheTransaction<K, V>) -> Boolean): Boolean =
        runNested(block) { keep -> keep }

    internal fun commitRoot() {
        check(frames.size == 1) { "expected depth 1 at commit, was ${frames.size}" }
        cache.applyAll(frames.removeLast())
    }

    internal fun abandon() {
        frames.clear()
    }

    private fun <R> runNested(
        block: (CacheTransaction<K, V>) -> R,
        keep: (R) -> Boolean
    ): R {
        checkOpen()
        frames.addLast(LinkedHashMap())

        val result = try {
            block(this)
        } catch (e: Throwable) {
            frames.removeLast()
            throw e
        }

        val frame = frames.removeLast()
        if (keep(result)) mergeDown(frame, into = frames.last())
        return result
    }

    private fun stage(key: K, write: Write<V>) {
        val frame = frames.last()
        frame.remove(key)
        frame[key] = write
    }

    private fun mergeDown(frame: Map<K, Write<V>>, into: LinkedHashMap<K, Write<V>>) {
        for ((key, write) in frame) {
            into.remove(key)
            into[key] = write
        }
    }

    private fun checkOpen() = check(isOpen) { "transaction already finished" }
}
