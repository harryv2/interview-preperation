package dsa.heap.generic

interface Heap<T> {
    val size: Int
    fun isEmpty(): Boolean = size == 0

    fun add(value: T)
    fun poll(): T
    fun peek(): T
}
