package dsa.mycodeschool.queue

import dsa.mycodeschool.linkedlist.DoublyLinkedList

class QueueLinkedListImplementation<T>: Queue<T> {

    var list = DoublyLinkedList<T>()

    override var size: Int = 0
        private set

    override fun enqueue(value: T) {
        list.addToListTail(value)
        size++
    }

    override fun dequeue(): T {
        check(size > 0) {"Queue is empty"}
        size--
        return list.removeFirst()
    }

    override fun peek(): T {
        check(size > 0) {"Queue is empty"}
        return list.peekFirst()!!
    }
}


fun main() {

    val queue = QueueLinkedListImplementation<Int>()

    queue.enqueue(1)
    queue.enqueue(2)
    queue.enqueue(3)


    while (!queue.isEmpty()) {
        println(queue.dequeue())
    }
}