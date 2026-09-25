package lld_self.cache.lru_ttl_txn

import kotlin.time.Duration

sealed interface Write<out V> {
    data class Put<out V>(val value: V, val ttl: Duration) : Write<V>
    data object Delete : Write<Nothing>
}
