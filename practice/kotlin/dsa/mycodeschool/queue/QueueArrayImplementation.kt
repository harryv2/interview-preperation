package dsa.mycodeschool.queue

class QueueArray<T>(initialCapacity: Int = 10) : Queue<T> {

    init {
        require(initialCapacity > 0) { "Capacity should be positive" }
    }

    private var arr: Array<Any?> = arrayOfNulls(initialCapacity)

    private var front = 0
    private var rear = -1

    override var size: Int = 0
        private set


    override fun enqueue(value: T) {
        if (size == arr.size) {
            grow()
        }

        rear = (rear + 1) % arr.size
        arr[rear] = value
        size++
    }

    override fun dequeue(): T {
        val value = peek()
        arr[front] = null
        front = (front + 1) % arr.size
        size--
        return value
    }

    @Suppress("UNCHECKED_CAST")
    override fun peek(): T {
        check(size > 0) { "Queue is empty" }

        return arr[front] as T
    }

    private fun grow() {
        val newArr = arrayOfNulls<Any?>(arr.size * 2)
        for (i in 0 until size) {
            newArr[i] = arr[(front + i) % arr.size]
        }
        arr = newArr
        front = 0
        rear = size - 1
    }

}

fun main() {

    val queue = QueueArray<Int>(4)

    queue.enqueue(1)
    queue.enqueue(2)
    queue.enqueue(3)

    println(queue.dequeue())
    println(queue.dequeue())

    queue.enqueue(4)
    queue.enqueue(5)
    queue.enqueue(6)   // wraps around to index 0
    queue.enqueue(7)   // full -> grows, unwrapping 3,4,5,6 to the front

    while (!queue.isEmpty()) {
        println(queue.dequeue())
    }
}
