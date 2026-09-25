package lld.fintech.volatilitymonitor.structure


// Sliding window extreme in O(1) amortised. The insight is that a sample can be thrown away the moment a
// later sample beats it: it is younger AND worse, so it can never be the answer again for any window that
// still contains the newer one. What survives is a monotonic run of candidates, and the front of it is the
// answer. Every sample is pushed once and popped once, so the per trade cost is constant even though a
// single push can pop many.
class MonotonicWindow(
    private val size: Int,
    private val beats: Comparator<Long>
) {

    private class Sample(val seq: Long, val value: Long)

    private val candidates = ArrayDeque<Sample>()
    private var seen = 0L

    init {
        require(size > 0) { "Window size must be positive" }
    }

    fun add(value: Long) {
        while (candidates.isNotEmpty() && beats.compare(value, candidates.last().value) >= 0) {
            candidates.removeLast()
        }

        candidates.addLast(Sample(seen, value))
        seen += 1
        evictLeavers()
    }

    fun extreme(): Long? {
        return candidates.firstOrNull()?.value
    }

    fun isEmpty(): Boolean = candidates.isEmpty()

    // Ages out by arrival number rather than by value. Comparing values would be wrong the moment the same
    // price arrives twice, since the copy leaving the window looks exactly like the copy still in it.
    private fun evictLeavers() {
        val oldestInWindow = seen - size
        while (candidates.isNotEmpty() && candidates.first().seq < oldestInWindow) {
            candidates.removeFirst()
        }
    }

    companion object {
        fun forMax(size: Int) = MonotonicWindow(size, naturalOrder())
        fun forMin(size: Int) = MonotonicWindow(size, reverseOrder())
    }
}
