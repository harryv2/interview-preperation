package lld_self.cache.lfu


class LfuCache<T, U>(val maxSize: Int) {

    data class CacheNode<U>(var data: U, var freq: Int)

    private val lock = Any()
    private val cacheMap = HashMap<T, CacheNode<U>>()
    private val freqToKeyMap = HashMap<Int, LinkedHashSet<T>>()
    private var minFrequency: Int = 0

    val size
        get() = synchronized(lock) { cacheMap.size }


    fun contains(key: T): Boolean = synchronized(lock) { cacheMap.containsKey(key) }

    fun frequency(key: T): Int? = synchronized(lock) { cacheMap[key]?.freq }

    fun keys(): List<T> = synchronized(lock) {
        freqToKeyMap.entries.sortedBy { it.key }.flatMap { it.value }
    }


    private fun evict() {
        if (cacheMap.isEmpty()) {
            return
        }

        val minKey = freqToKeyMap[minFrequency]?.firstOrNull() ?: return
        cacheMap.remove(minKey)
        freqToKeyMap[minFrequency]?.remove(minKey)

        if (freqToKeyMap[minFrequency]?.isEmpty() == true) {
            freqToKeyMap.remove(minFrequency)
        }
    }

    fun add(key: T, value: U) {
        synchronized(lock) {
            if (cacheMap.containsKey(key)) {
                val node = cacheMap[key]!!
                node.data = value
                updateFrequency(key, node)
                return
            }

            if (cacheMap.size >= maxSize) {
                evict()
            }

            val node = CacheNode(value, 1)
            minFrequency = 1
            val freToKeyMapList = freqToKeyMap.getOrDefault(minFrequency, LinkedHashSet())
            freToKeyMapList.add(key)
            freqToKeyMap[minFrequency] = freToKeyMapList
            cacheMap[key] = node
        }
    }

    private fun updateFrequency(key: T, node: CacheNode<U>) {
        val oldFreq = node.freq
        val oldList = freqToKeyMap.getOrDefault(oldFreq, LinkedHashSet())
        oldList.remove(key)

        if (oldList.isEmpty()) {
            freqToKeyMap.remove(oldFreq)

            if (minFrequency == oldFreq) {
                minFrequency++
            }
        }

        val newFreq = oldFreq + 1
        val newList = freqToKeyMap.getOrDefault(newFreq, LinkedHashSet())
        newList.add(key)
        freqToKeyMap[newFreq] = newList

        node.freq++

    }

    fun get(key: T): U? {
        synchronized(lock) {
            val node = cacheMap[key] ?: return null
            updateFrequency(key, node)
            return node.data
        }
    }
}
