@file:OptIn(ExperimentalTime::class)

package lld_self.cache.lru_ttl_txn

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class FlatCacheTransaction<K, V> internal constructor(
    private val cache: LRUCacheTTL<K, V>
) {
    private val buffer = LinkedHashMap<K, Write<V>>()
    private var finished = false

    val isOpen: Boolean get() = !finished

    fun get(key: K): V? {
        checkOpen()
        return when (val write = buffer[key]) {
            is Write.Put -> write.value
            Write.Delete -> null
            null -> cache.get(key)
        }
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

    private fun stage(key: K, write: Write<V>) {
        buffer.remove(key)
        buffer[key] = write
    }

    fun commit() {
        checkOpen()
        cache.applyAll(buffer)
        finished = true
    }

    fun rollback() {
        checkOpen()
        buffer.clear()
        finished = true
    }

    private fun checkOpen() = check(!finished) { "transaction already finished" }
}

fun <K, V, R> LRUCacheTTL<K, V>.runInFlatTransaction(
    block: (FlatCacheTransaction<K, V>) -> R
): R {
    val txn = FlatCacheTransaction(this)
    return try {
        val result = block(txn)
        if (txn.isOpen) txn.commit()
        result
    } catch (e: Throwable) {
        if (txn.isOpen) txn.rollback()
        throw e
    }
}
