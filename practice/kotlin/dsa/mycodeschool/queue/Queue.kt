package dsa.mycodeschool.queue

interface Queue<T> {
    val size: Int
    fun isEmpty(): Boolean {
        return size == 0
    }
    fun enqueue(value: T)
    fun dequeue(): T
    fun peek(): T
}