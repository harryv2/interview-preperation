package dsa.heap.generic

// The comparator decides priority the same way java.util.PriorityQueue does:
// whichever element compare() says is "smaller" comes out first.
//   BinaryHeap(compareBy { it })              min-heap
//   BinaryHeap(compareByDescending { it })    max-heap
//   BinaryHeap(compareBy { it.distance })     Dijkstra-style, smallest distance first
class BinaryHeap<T>(
    private val comparator: Comparator<T>,
    initialSize: Int = 10
) : Heap<T> {

    init {
        require(initialSize > 0) { "Initial size should be positive" }
    }

    // same trick as StackArray / QueueArray: store Any?, cast on read
    private var arr: Array<Any?> = arrayOfNulls(initialSize)

    private var top = -1

    // O(n) build from an existing collection
    constructor(values: Collection<T>, comparator: Comparator<T>) : this(comparator, maxOf(values.size, 1)) {
        values.forEachIndexed { i, v -> arr[i] = v }
        top = values.size - 1

        for (i in (top - 1) / 2 downTo 0) {
            siftDown(i)
        }
    }


    override val size: Int
        get() = top + 1


    @Suppress("UNCHECKED_CAST")
    private fun at(i: Int): T = arr[i] as T

    private fun grow() {
        arr = arr.copyOf(arr.size * 2)
    }

    private fun swap(i: Int, j: Int) {
        val temp = arr[i]
        arr[i] = arr[j]
        arr[j] = temp
    }

    // true if arr[i] should sit above arr[j]
    private fun compare(i: Int, j: Int): Boolean {
        return comparator.compare(at(i), at(j)) <= 0
    }


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

    private fun siftDown(start: Int) {
        var i = start
        while (2 * i + 1 <= top) {
            val p1 = 2 * i + 1
            val p2 = 2 * i + 2

            var best = p1
            if (p2 <= top && compare(p2, best)) {
                best = p2
            }

            if (compare(i, best)) {
                break
            }

            swap(i, best)
            i = best
        }
    }


    override fun add(value: T) {
        if (arr.lastIndex == top) {
            grow()
        }

        top++
        arr[top] = value
        siftUp(top)
    }

    override fun poll(): T {
        require(!isEmpty()) { "Heap is empty" }

        val answer = at(0)
        swap(0, top)
        arr[top] = null                     // let the object be collected
        top--
        siftDown(0)

        return answer
    }

    override fun peek(): T {
        require(!isEmpty()) { "Heap is empty" }

        return at(0)
    }

}


fun main() {

    val minHeap = BinaryHeap<Int>(compareBy { it })
    for (v in listOf(5, 1, 4, 2, 3)) {
        minHeap.add(v)
    }
    print("min: ")
    while (!minHeap.isEmpty()) {
        print("${minHeap.poll()} ")
    }
    println()

    val maxHeap = BinaryHeap(listOf(5, 1, 4, 2, 3), compareByDescending<Int> { it })
    print("max (built from list): ")
    while (!maxHeap.isEmpty()) {
        print("${maxHeap.poll()} ")
    }
    println()

    // objects with a custom priority, like Dijkstra's (vertex, distance)
    data class Item(val name: String, val priority: Int)

    val byPriority = BinaryHeap<Item>(compareBy { it.priority })
    byPriority.add(Item("low", 30))
    byPriority.add(Item("high", 5))
    byPriority.add(Item("mid", 12))
    println("peek = ${byPriority.peek()}")
    while (!byPriority.isEmpty()) {
        println("  ${byPriority.poll()}")
    }

}
