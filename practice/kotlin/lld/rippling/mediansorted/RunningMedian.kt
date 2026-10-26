package lld.rippling.mediansorted

import java.util.PriorityQueue

class RunningMedian {
    private val low = PriorityQueue<Int>(reverseOrder())
    private val high = PriorityQueue<Int>()

    val size: Int
        get() = low.size + high.size

    fun add(value: Int) {
        if (low.isEmpty() || value <= low.peek()) {
            low += value
        } else {
            high += value
        }

        if (low.size > high.size + 1) {
            high += low.poll()
        } else if (high.size > low.size) {
            low += high.poll()
        }
    }

    fun median(): Double {
        check(size > 0) { "No values yet" }
        if (low.size > high.size) {
            return low.peek().toDouble()
        }
        return (low.peek().toLong() + high.peek()) / 2.0
    }
}
