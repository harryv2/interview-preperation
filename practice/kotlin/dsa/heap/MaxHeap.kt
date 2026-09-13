package dsa.heap


class MaxHeap(initialSize: Int = 10) : Heap {

    init {
        require(initialSize > 0) { "Initial size should be positive" }
    }

    private var arr = IntArray(initialSize)

    private var top = -1

    // Build a heap from an existing array in O(n): copy it in, then sift down every
    // non-leaf from the last one up to the root. Leaves (the back half) are already
    // valid one-element heaps, so they need no work.
    constructor(values: IntArray) : this(maxOf(values.size, 1)) {
        values.copyInto(arr)
        top = values.size - 1

        for (i in (top - 1) / 2 downTo 0) {
            siftDown(i)
        }
    }


    override val size: Int
        get() = top + 1


    private fun grow() {
        arr = arr.copyOf(arr.size * 2)
    }

    private fun swap(i: Int, j: Int) {
        val temp = arr[i]
        arr[i] = arr[j]
        arr[j] = temp
    }

    private fun compare(i: Int, j: Int): Boolean {
        return arr[i] >= arr[j]
    }


    // move arr[i] up while it is bigger than its parent
    private fun siftUp(start: Int) {
        var i = start
        while (i > 0) {
            val parent = (i - 1) / 2
            if (compare(parent, i)) {
                break
            }

            swap(parent, i)
            i = parent
        }
    }

    // move arr[i] down while a child is bigger than it
    private fun siftDown(start: Int) {
        var i = start
        while (2 * i + 1 <= top) {          // i has at least a left child
            val p1 = 2 * i + 1
            val p2 = 2 * i + 2

            // pick the bigger child; p1 always exists here, p2 may not
            var largest = p1
            if (p2 <= top && compare(p2, largest)) {
                largest = p2
            }

            if (compare(i, largest)) {
                break
            }

            swap(i, largest)
            i = largest
        }
    }


    override fun add(value: Int) {
        if (arr.lastIndex == top) {
            grow()
        }

        top++
        arr[top] = value
        siftUp(top)
    }

    override fun poll(): Int {
        require(!isEmpty()) { "Heap is empty" }

        val answer = arr[0]
        swap(0, top)
        top--
        siftDown(0)

        return answer
    }


}


fun main() {
    val heap = MaxHeap()

    heap.add(1)

    heap.add(3)

    heap.add(2)


    heap.add(4)


    heap.add(5)

    while (!heap.isEmpty()) {
        println(heap.poll())
    }

    println("---")

    // no sift-ups on insert, so the array is exactly this order; exercises the two-children path
    val heap2 = MaxHeap(4)
    for (v in listOf(9, 5, 8, 1, 2, 7, 6, 0, 0)) {
        heap2.add(v)
    }

    while (!heap2.isEmpty()) {
        print("${heap2.poll()} ")
    }
    println()

    println("--- built from array")
    val heap3 = MaxHeap(intArrayOf(3, 1, 4, 1, 5, 9, 2, 6))

    while (!heap3.isEmpty()) {
        print("${heap3.poll()} ")
    }
    println()
}
