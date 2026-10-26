package lld.rippling.kvstore

class KeyValueStore {
    private val data = HashMap<String, String>()
    private val transactions = ArrayDeque<HashMap<String, String?>>()

    fun get(key: String): String? {
        for (layer in transactions.asReversed()) {
            if (layer.containsKey(key)) {
                return layer[key]
            }
        }
        return data[key]
    }

    fun set(key: String, value: String) {
        write(key, value)
    }

    fun delete(key: String): Boolean {
        if (get(key) == null) {
            return false
        }
        write(key, null)
        return true
    }

    fun begin() {
        transactions.addLast(HashMap())
    }

    fun commit() {
        val top = transactions.removeLastOrNull() ?: throw IllegalStateException("No active transaction")
        val parent = transactions.lastOrNull()
        if (parent != null) {
            parent.putAll(top)
            return
        }
        for ((key, value) in top) {
            if (value == null) {
                data.remove(key)
            } else {
                data[key] = value
            }
        }
    }

    fun rollback() {
        transactions.removeLastOrNull() ?: throw IllegalStateException("No active transaction")
    }

    private fun write(key: String, value: String?) {
        val top = transactions.lastOrNull()
        if (top != null) {
            top[key] = value
            return
        }
        if (value == null) {
            data.remove(key)
        } else {
            data[key] = value
        }
    }
}
