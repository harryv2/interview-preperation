package lld.leaderboard.structure

import java.util.TreeSet


// the obvious Java answer, kept here to be rejected. TreeSet orders in O(log n) but stores no subtree sizes,
// so it can tell you what is next and never what position you are in without counting your way there.
class TreeSetRankedSet<T>(comparator: Comparator<T>) : RankedSet<T> {

    private val set = TreeSet(comparator)

    override val size: Int
        get() = set.size

    override fun insert(value: T) {
        set.remove(value)
        set.add(value)
    }

    override fun remove(value: T): Boolean {
        return set.remove(value)
    }

    // O(n): headSet hands back a view, and asking a view for its size walks every element in it
    override fun rankOf(value: T): Int {
        if (!set.contains(value)) {
            return -1
        }
        return set.headSet(value).size
    }

    // O(n): there is no way into the middle of a TreeSet except walking to it from one end
    override fun select(index: Int): T? {
        if (index < 0 || index >= set.size) {
            return null
        }

        val iterator = set.iterator()
        repeat(index) { iterator.next() }
        return iterator.next()
    }

    override fun range(from: Int, count: Int): List<T> {
        if (from < 0 || count <= 0) {
            return emptyList()
        }
        return set.asSequence().drop(from).take(count).toList()
    }
}
