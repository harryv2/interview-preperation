package dsa.leetcode_75.mycodeschool.stack

interface Stack<T> {
    val size: Int
    fun isEmpty(): Boolean = size == 0
    fun push(value: T)
    fun pop(): T
    fun top(): T
}
