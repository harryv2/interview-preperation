package dsa.heap

interface Heap {
    val size: Int
    fun isEmpty(): Boolean = size == 0

    fun add(value: Int)
    fun poll(): Int

}