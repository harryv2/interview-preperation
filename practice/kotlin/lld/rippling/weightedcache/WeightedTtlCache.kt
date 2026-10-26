package lld.rippling.weightedcache

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class WeightedTtlCache<K, V>(
    private val maxWeight: Long,
    sweepEveryMillis: Long = 1000,
    private val clock: () -> Long = System::currentTimeMillis,
) : AutoCloseable {

    private class Entry<V>(val value: V, val weight: Long, val expiresAt: Long)

    private val lock = ReentrantLock()
    private val entries = LinkedHashMap<K, Entry<V>>(16, 0.75f, true)
    private var totalWeight = 0L
    private val listeners = ArrayList<(K, String) -> Unit>()

    private val sweeper = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "cache-sweeper").apply { isDaemon = true }
    }

    init {
        require(maxWeight > 0) { "Max weight must be positive" }
        sweeper.scheduleAtFixedRate({ cleanup() }, sweepEveryMillis, sweepEveryMillis, TimeUnit.MILLISECONDS)
    }

    val weight: Long
        get() = lock.withLock { totalWeight }

    val size: Int
        get() = lock.withLock { entries.size }

    fun onEvict(listener: (K, String) -> Unit) {
        listeners += listener
    }

    fun put(key: K, value: V, weight: Long, ttlMillis: Long) {
        require(weight in 1..maxWeight) { "Weight $weight must be in 1..$maxWeight" }
        require(ttlMillis > 0) { "TTL must be positive" }
        val overLimit = lock.withLock {
            entries.remove(key)?.let { totalWeight -= it.weight }
            entries[key] = Entry(value, weight, clock() + ttlMillis)
            totalWeight += weight
            totalWeight > maxWeight
        }
        if (overLimit) {
            sweeper.execute { cleanup() }
        }
    }

    fun get(key: K): V? {
        lock.withLock {
            val entry = entries[key] ?: return null
            if (entry.expiresAt <= clock()) {
                entries.remove(key)
                totalWeight -= entry.weight
                return null
            }
            return entry.value
        }
    }

    fun remove(key: K): V? {
        lock.withLock {
            val entry = entries.remove(key) ?: return null
            totalWeight -= entry.weight
            return entry.value
        }
    }

    fun cleanup() {
        val evicted = ArrayList<Pair<K, String>>()
        lock.withLock {
            val now = clock()
            val iterator = entries.entries.iterator()
            while (iterator.hasNext()) {
                val (key, entry) = iterator.next()
                if (entry.expiresAt <= now) {
                    iterator.remove()
                    totalWeight -= entry.weight
                    evicted += key to "expired"
                }
            }

            val lru = entries.entries.iterator()
            while (totalWeight > maxWeight && lru.hasNext()) {
                val (key, entry) = lru.next()
                lru.remove()
                totalWeight -= entry.weight
                evicted += key to "weight"
            }
        }
        for ((key, reason) in evicted) {
            listeners.forEach { it(key, reason) }
        }
    }

    override fun close() {
        sweeper.shutdownNow()
    }
}
