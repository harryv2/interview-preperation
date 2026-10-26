package lld.rippling.musicanalytics

import java.util.PriorityQueue

class IndexedPriorityQueue<K, T>(
    private val comparator: Comparator<in T>,
    private val keyOf: (T) -> K,
) {
    private val heap = ArrayList<T>()
    private val position = HashMap<K, Int>()

    val size: Int
        get() = heap.size

    fun isEmpty(): Boolean {
        return heap.isEmpty()
    }

    operator fun contains(key: K): Boolean {
        return position.containsKey(key)
    }

    fun add(item: T) {
        val key = keyOf(item)
        require(!position.containsKey(key)) { "Key $key already in queue" }
        heap += item
        position[key] = heap.size - 1
        siftUp(heap.size - 1)
    }

    fun update(item: T) {
        val key = keyOf(item)
        val index = requireNotNull(position[key]) { "Key $key not in queue" }
        heap[index] = item
        siftUp(index)
        siftDown(position.getValue(key))
    }

    fun addOrUpdate(item: T) {
        if (keyOf(item) in this) {
            update(item)
        } else {
            add(item)
        }
    }

    fun peek(): T? {
        return heap.firstOrNull()
    }

    fun poll(): T? {
        if (heap.isEmpty()) {
            return null
        }
        return removeAt(0)
    }

    fun remove(key: K): T? {
        val index = position[key] ?: return null
        return removeAt(index)
    }

    fun top(k: Int): List<T> {
        val result = ArrayList<T>()
        if (heap.isEmpty()) {
            return result
        }
        val frontier = PriorityQueue<Int> { a, b -> comparator.compare(heap[a], heap[b]) }
        frontier += 0
        while (result.size < k && frontier.isNotEmpty()) {
            val index = frontier.poll()
            result += heap[index]
            val left = 2 * index + 1
            if (left < heap.size) {
                frontier += left
            }
            if (left + 1 < heap.size) {
                frontier += left + 1
            }
        }
        return result
    }

    fun get(key: K): T? {
        val index = position[key] ?: return null
        return heap[index]
    }

    private fun removeAt(index: Int): T {
        val item = heap[index]
        val last = heap.size - 1
        swap(index, last)
        heap.removeAt(last)
        position.remove(keyOf(item))
        if (index < heap.size) {
            siftUp(index)
            siftDown(index)
        }
        return item
    }

    private fun siftUp(start: Int) {
        var index = start
        while (index > 0) {
            val parent = (index - 1) / 2
            if (comparator.compare(heap[index], heap[parent]) >= 0) {
                return
            }
            swap(index, parent)
            index = parent
        }
    }

    private fun siftDown(start: Int) {
        var index = start
        while (true) {
            val left = 2 * index + 1
            val right = left + 1
            var best = index
            if (left < heap.size && comparator.compare(heap[left], heap[best]) < 0) {
                best = left
            }
            if (right < heap.size && comparator.compare(heap[right], heap[best]) < 0) {
                best = right
            }
            if (best == index) {
                return
            }
            swap(index, best)
            index = best
        }
    }

    private fun swap(i: Int, j: Int) {
        val a = heap[i]
        val b = heap[j]
        heap[i] = b
        heap[j] = a
        position[keyOf(b)] = i
        position[keyOf(a)] = j
    }
}
