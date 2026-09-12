package dsa.leetcode_75.mycodeschool.stack

class StackArray<T>(initialCapacity: Int = 10) : Stack<T> {

    private var arr: Array<Any?> = arrayOfNulls(initialCapacity)

    override var size: Int = 0
        private set

    override fun push(value: T) {
        if (size == arr.size) {
            arr = arr.copyOf(arr.size * 2)
        }
        arr[size] = value
        size++
    }

    override fun pop(): T {
        val value = top()
        size--
        arr[size] = null
        return value
    }

    @Suppress("UNCHECKED_CAST")
    override fun top(): T {
        check(size > 0) { "Stack is empty" }
        return arr[size - 1] as T
    }
}


fun main() {

    var stack = StackArray<Int>()

    stack.push(10)

    stack.push(20)

    stack.push(30)

    while (!stack.isEmpty()){
        println(stack.pop())
    }
}