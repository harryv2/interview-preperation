@file:OptIn(ExperimentalTime::class)

package lld_self.cache.lru_ttl

import lld_self.cache.lru.DoublyLinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
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
    override fun now(): Instant {
        return kotlin.time.Clock.System.now()
    }
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

    val size
        get() = cacheMap.size

    fun contains(key: T): Boolean = lock.withLock { cacheMap.containsKey(key) }


    private fun evict() {
        if (cacheMap.isEmpty()) {
            return
        }

        val nodeToDelete = doublyList.removeFirst()
        cacheMap.remove(nodeToDelete.key)
        ttlMap.remove(nodeToDelete.key)
    }

    fun add(key: T, value: U, ttl: Duration) {
        lock.withLock {
            var now = clock.now()

            if (contains(key)) {
                val node = cacheMap[key]!!
                node.value = value
                doublyList.removeNode(node)
                doublyList.addNodeLast(node)
                ttlMap[key] = now + ttl
                return
            }

            if (cacheMap.size >= maxSize) {
                evict()
            }

            val node = DoublyLinkedList.Node<T, U>(key, value)
            doublyList.addNodeLast(node)
            cacheMap[key] = node
            ttlMap[key] = now + ttl
        }
    }


    fun get(key: T): U? {
        lock.withLock {
            val node = cacheMap[key] ?: return null

            val ttl = ttlMap[key]!!

            var now = clock.now()
            if (ttl <= now) {
                deleteKey(node)
                return null
            }

            doublyList.removeNode(node)
            doublyList.addNodeLast(node)

            return node.value
        }
    }

    private fun sweep() {
        var now = clock.now()
        lock.withLock {
            var keysToDelete = cacheMap.filter { (t, node) ->
                ttlMap[t]!! <= now
            }

            keysToDelete.forEach { (t, node) ->
                deleteKey(node)
            }
        }
    }

    private fun deleteKey(node: DoublyLinkedList.Node<T, U>) {
        doublyList.removeNode(node)
        cacheMap.remove(node.key)
        ttlMap.remove(node.key)
    }


    override fun close() {
        sweeper.shutdownNow()
    }
}