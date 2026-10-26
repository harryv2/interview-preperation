package lld.rippling.aliendictionary

object AlienDictionary {

    fun order(words: List<String>): String {
        val edges = HashMap<Char, HashSet<Char>>()
        val inDegree = HashMap<Char, Int>()
        for (word in words) {
            for (c in word) {
                inDegree.putIfAbsent(c, 0)
            }
        }

        for (i in 0 until words.size - 1) {
            val first = words[i]
            val second = words[i + 1]
            val diff = first.indices.firstOrNull { it < second.length && first[it] != second[it] }
            if (diff == null) {
                if (first.length > second.length) {
                    return ""
                }
                continue
            }
            if (edges.getOrPut(first[diff]) { HashSet() }.add(second[diff])) {
                inDegree[second[diff]] = inDegree.getValue(second[diff]) + 1
            }
        }

        val queue = ArrayDeque(inDegree.filterValues { it == 0 }.keys.sorted())
        val result = StringBuilder()
        while (queue.isNotEmpty()) {
            val c = queue.removeFirst()
            result.append(c)
            for (next in edges[c].orEmpty().sorted()) {
                val remaining = inDegree.getValue(next) - 1
                inDegree[next] = remaining
                if (remaining == 0) {
                    queue.addLast(next)
                }
            }
        }

        return if (result.length == inDegree.size) result.toString() else ""
    }
}
