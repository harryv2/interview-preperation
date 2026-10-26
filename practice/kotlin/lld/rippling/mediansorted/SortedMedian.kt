package lld.rippling.mediansorted

import java.util.PriorityQueue

object SortedMedian {

    fun ofTwo(a: IntArray, b: IntArray): Double {
        if (a.size > b.size) {
            return ofTwo(b, a)
        }
        val total = a.size + b.size
        require(total > 0) { "Both arrays are empty" }
        val half = (total + 1) / 2

        var lo = 0
        var hi = a.size
        while (lo <= hi) {
            val i = (lo + hi) / 2
            val j = half - i
            val aLeft = if (i == 0) Int.MIN_VALUE else a[i - 1]
            val aRight = if (i == a.size) Int.MAX_VALUE else a[i]
            val bLeft = if (j == 0) Int.MIN_VALUE else b[j - 1]
            val bRight = if (j == b.size) Int.MAX_VALUE else b[j]

            if (aLeft > bRight) {
                hi = i - 1
            } else if (bLeft > aRight) {
                lo = i + 1
            } else {
                val leftMax = maxOf(aLeft, bLeft)
                if (total % 2 == 1) {
                    return leftMax.toDouble()
                }
                return (leftMax.toLong() + minOf(aRight, bRight)) / 2.0
            }
        }
        throw IllegalStateException("Arrays are not sorted")
    }

    fun ofMany(arrays: List<IntArray>): Double {
        val total = arrays.sumOf { it.size }
        require(total > 0) { "All arrays are empty" }
        if (total % 2 == 1) {
            return kthSmallest(arrays, total / 2 + 1).toDouble()
        }
        return (kthSmallest(arrays, total / 2).toLong() + kthSmallest(arrays, total / 2 + 1)) / 2.0
    }

    fun ofStreams(streams: List<Iterator<Int>>, total: Int): Double {
        require(total > 0) { "Total must be positive" }
        val heap = PriorityQueue<Pair<Int, Int>>(compareBy { it.first })
        streams.forEachIndexed { index, stream ->
            if (stream.hasNext()) {
                heap += stream.next() to index
            }
        }

        val lowerIndex = (total - 1) / 2
        val upperIndex = total / 2
        var lower = 0
        var position = 0
        while (heap.isNotEmpty()) {
            val (value, index) = heap.poll()
            if (position == lowerIndex) {
                lower = value
            }
            if (position == upperIndex) {
                return (lower.toLong() + value) / 2.0
            }
            position++
            if (streams[index].hasNext()) {
                heap += streams[index].next() to index
            }
        }
        throw IllegalArgumentException("Streams hold fewer than $total values")
    }

    private fun kthSmallest(arrays: List<IntArray>, k: Int): Int {
        var lo = arrays.filter { it.isNotEmpty() }.minOf { it.first() }.toLong()
        var hi = arrays.filter { it.isNotEmpty() }.maxOf { it.last() }.toLong()
        while (lo < hi) {
            val mid = lo + (hi - lo) / 2
            val count = arrays.sumOf { countAtMost(it, mid) }
            if (count >= k) {
                hi = mid
            } else {
                lo = mid + 1
            }
        }
        return lo.toInt()
    }

    private fun countAtMost(array: IntArray, value: Long): Int {
        var lo = 0
        var hi = array.size
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (array[mid] <= value) {
                lo = mid + 1
            } else {
                hi = mid
            }
        }
        return lo
    }
}
