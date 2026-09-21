package lld_self.cache.lru

import java.util.concurrent.ConcurrentHashMap


class LRUCache<T, U>(val maxSize: Int) {

    private val cacheMap = ConcurrentHashMap<T, DoublyLinkedList.Node<T, U>>()
    private val doublyList = DoublyLinkedList<T, U>()

    val size
        get() = cacheMap.size

    fun contains(key: T): Boolean = cacheMap.containsKey(key)

    fun keys(): List<T> {
        val keys = mutableListOf<T>()
        var node = doublyList.head
        while (node != null) {
            keys.add(node.key)
            node = node.next
        }
        return keys
    }


    private fun evict() {
        if (cacheMap.isEmpty()) {
            return
        }

        val nodeToDelete = doublyList.removeFirst()
        cacheMap.remove(nodeToDelete.key)
    }

    fun add(key: T, value: U) {
        if (contains(key)) {
            val node = cacheMap[key]!!
            node.value = value
            doublyList.removeNode(node)
            doublyList.addNodeLast(node)
            return
        }

        if (cacheMap.size >= maxSize) {
            evict()
        }

        val node = DoublyLinkedList.Node<T, U>(key, value)
        doublyList.addNodeLast(node)
        cacheMap[key] = node
    }


    fun get(key: T): U? {
        if (!contains(key)) {
            return null
        }

        val node = cacheMap[key]!!

        doublyList.removeNode(node)
        doublyList.addNodeLast(node)

        return node.value
    }

}