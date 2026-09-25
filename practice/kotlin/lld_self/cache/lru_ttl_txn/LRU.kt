@file:OptIn(ExperimentalTime::class)

package lld_self.cache.lru_ttl_txn

import lld_self.cache.lru.DoublyLinkedList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface Clock {
    fun now(): Instant
}

class SystemClock : Clock {
    override fun now(): Instant = kotlin.time.Clock.System.now()
}

class LRUCacheTTL<T, U>(
    val maxSize: Int,
    val clock: Clock = SystemClock()
) : AutoCloseable {
    private val cacheMap = HashMap<T, DoublyLinkedList.Node<T, U>>()
    private val doublyList = DoublyLinkedList<T, U>()
    private val ttlMap = HashMap<T, Instant>()

    private val lock = ReentrantLock()

    private val sweeper = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "ttl-sweeper").apply { isDaemon = true }
    }

    init {
        sweeper.scheduleAtFixedRate({ sweep() }, 10, 10, TimeUnit.SECONDS)
    }

    val size: Int
        get() = lock.withLock { cacheMap.size }

    fun contains(key: T): Boolean = lock.withLock {
        val deadline = ttlMap[key]
        deadline != null && deadline > clock.now()
    }

    fun add(key: T, value: U, ttl: Duration) = lock.withLock {
        putLocked(key, value, clock.now() + ttl)
    }

    fun get(key: T): U? = lock.withLock { getLocked(key) }

    fun getAll(keys: Collection<T>): Map<T, U> = lock.withLock {
        buildMap {
            for (key in keys) getLocked(key)?.let { put(key, it) }
        }
    }

    fun delete(key: T) = lock.withLock {
        cacheMap[key]?.let { deleteKey(it) }
        Unit
    }

    internal fun applyAll(writes: Map<T, Write<U>>) = lock.withLock {
        val now = clock.now()
        for ((key, write) in writes) {
            when (write) {
                is Write.Put -> putLocked(key, write.value, now + write.ttl)
                is Write.Delete -> cacheMap[key]?.let { deleteKey(it) }
            }
        }
    }

    fun <R> runInTransaction(block: (CacheTransaction<T, U>) -> R): R {
        val txn = CacheTransaction(this)
        return try {
            val result = block(txn)
            txn.commitRoot()
            result
        } catch (e: Throwable) {
            txn.abandon()
            throw e
        }
    }

    private fun getLocked(key: T): U? {
        val node = cacheMap[key] ?: return null
        val deadline = ttlMap[key]!!

        if (deadline <= clock.now()) {
            deleteKey(node)
            return null
        }

        doublyList.removeNode(node)
        doublyList.addNodeLast(node)
        return node.value
    }

    private fun putLocked(key: T, value: U, deadline: Instant) {
        val existing = cacheMap[key]
        if (existing != null) {
            existing.value = value
            doublyList.removeNode(existing)
            doublyList.addNodeLast(existing)
            ttlMap[key] = deadline
            return
        }

        if (cacheMap.size >= maxSize) evict()

        val node = DoublyLinkedList.Node<T, U>(key, value)
        doublyList.addNodeLast(node)
        cacheMap[key] = node
        ttlMap[key] = deadline
    }

    private fun evict() {
        if (cacheMap.isEmpty()) return
        val node = doublyList.removeFirst()
        cacheMap.remove(node.key)
        ttlMap.remove(node.key)
    }

    private fun deleteKey(node: DoublyLinkedList.Node<T, U>) {
        doublyList.removeNode(node)
        cacheMap.remove(node.key)
        ttlMap.remove(node.key)
    }

    private fun sweep() = lock.withLock {
        val now = clock.now()
        val expired = cacheMap.filter { (key, _) -> ttlMap[key]!! <= now }
        expired.forEach { (_, node) -> deleteKey(node) }
    }

    override fun close() {
        sweeper.shutdownNow()
    }
}
