package dsa.mycodeschool.stack

import dsa.mycodeschool.linkedlist.DoublyLinkedList


class StackLinkedList<T>() : Stack<T> {

    var list = DoublyLinkedList<T>()

    override var size = 0
        private set


    override fun push(value: T) {
        list.addToListAtHead(value)
        size++
    }

    override fun pop(): T {
        var value = list.removeFirst()
        size--
        return value
    }

    override fun top(): T {
        require(size > 0) {"Empty stack"}
        return list.peekFirst()!!
    }

}

fun main() {

    var stack = StackLinkedList<Int>()

    stack.push(10)

    stack.push(20)

    stack.push(30)

    while (!stack.isEmpty()){
        println(stack.pop())
    }
}