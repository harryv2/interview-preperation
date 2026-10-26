package lld.rippling.lfucache

class LfuCache<K, V>(private val capacity: Int) {
    private class Entry<V>(var value: V, var freq: Int)

    private val entries = HashMap<K, Entry<V>>()
    private val keysByFreq = HashMap<Int, LinkedHashSet<K>>()
    private var minFreq = 0

    init {
        require(capacity > 0) { "Capacity must be positive" }
    }

    val size: Int
        get() = entries.size

    fun get(key: K): V? {
        val entry = entries[key] ?: return null
        touch(key, entry)
        return entry.value
    }

    fun put(key: K, value: V) {
        val existing = entries[key]
        if (existing != null) {
            existing.value = value
            touch(key, existing)
            return
        }

        if (entries.size == capacity) {
            evict()
        }

        entries[key] = Entry(value, 1)
        keysByFreq.getOrPut(1) { LinkedHashSet() }.add(key)
        minFreq = 1
    }

    private fun touch(key: K, entry: Entry<V>) {
        val bucket = keysByFreq.getValue(entry.freq)
        bucket.remove(key)
        if (bucket.isEmpty()) {
            keysByFreq.remove(entry.freq)
            if (minFreq == entry.freq) {
                minFreq++
            }
        }
        entry.freq++
        keysByFreq.getOrPut(entry.freq) { LinkedHashSet() }.add(key)
    }

    private fun evict() {
        val bucket = keysByFreq.getValue(minFreq)
        val victim = bucket.first()
        bucket.remove(victim)
        if (bucket.isEmpty()) {
            keysByFreq.remove(minFreq)
        }
        entries.remove(victim)
    }
}
