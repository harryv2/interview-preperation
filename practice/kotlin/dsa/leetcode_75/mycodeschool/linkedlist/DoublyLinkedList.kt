package dsa.leetcode_75.mycodeschool.linkedlist

class DoublyLinkedList<T> {

    private class Node<T>(val value: T, var prev: Node<T>? = null, var next: Node<T>? = null)

    private var head: Node<T>? = null
    private var tail: Node<T>? = null

    var size: Int = 0
        private set

    fun isEmpty() = size == 0

    fun peekFirst(): T? = head?.value

    fun peekLast(): T? = tail?.value


    fun addToListAtHead(value: T) {
        val node = Node(value, next = head)
        head?.prev = node
        head = node
        if (tail == null) {
            tail = node
        }

        size++
    }


    fun addToListTail(value: T) {
        val node = Node(value, prev = tail)
        tail?.next = node
        tail = node
        if (head == null) {
            head = node
        }

        size++
    }

    fun addAtIndex(value: T, index: Int) {
        require(index in 0..size) { "index $index out of bounds" }

        if (index == 0) {
            addToListAtHead(value)
            return
        }
        if (index == size) {
            addToListTail(value)
            return
        }

        val next = nodeAtIndex(index)
        val prev = next.prev!!
        val node = Node(value, prev = prev, next = next)
        prev.next = node
        next.prev = node

        size++
    }

    private inline fun collect(start: Node<T>?, step: (Node<T>) -> Node<T>?): List<T> {
        val list = ArrayList<T>(size)
        var current = start
        while (current != null) {
            list.add(current.value)
            current = step(current)
        }
        return list
    }

    fun toList() = collect(head) { it.next }
    fun toReversedList() = collect(tail) { it.prev }


    private fun nodeAtIndex(index: Int): Node<T> {
        require(index in 0 until size) { "index $index out of bounds" }

        var current: Node<T>
        if (index < size / 2) {
            current = head!!
            repeat(index) { current = current.next!! }
        } else {
            current = tail!!
            repeat(size - 1 - index) { current = current.prev!! }
        }
        return current
    }

    operator fun get(index: Int): T = nodeAtIndex(index).value

    private fun unlink(node: Node<T>): T {
        val prev = node.prev
        val next = node.next

        prev?.next = next
        next?.prev = prev

        if (node === head) {
            head = next
        }
        if (node === tail) {
            tail = prev
        }

        size--
        return node.value
    }

    fun removeAtIndex(index: Int): T = unlink(nodeAtIndex(index))

    fun removeFirst(): T {
        val first = requireNotNull(head) { "List is empty" }
        return unlink(first)
    }

    fun removeLast(): T {
        val last = requireNotNull(tail) { "List is empty" }
        return unlink(last)
    }

    private fun findNode(value: T): Node<T>? {
        var current = head
        while (current != null) {
            if (current.value == value) {
                return current
            }
            current = current.next
        }

        return null
    }

    fun indexOf(value: T): Int {
        var i = 0
        var current = head
        while (current != null) {
            if (current.value == value) {
                return i
            }
            i++
            current = current.next
        }

        return -1
    }

    operator fun contains(value: T) = findNode(value) != null

    fun removeValue(value: T): T {
        val node = requireNotNull(findNode(value)) { "Value not found $value" }
        return unlink(node)
    }

    fun clear() {
        head = null
        tail = null
        size = 0
    }
}



fun main() {

    val list = DoublyLinkedList<Int>()

    list.addToListAtHead(5)

    list.addToListTail(10)
    list.addToListTail(20)

    list.addToListTail(30)

    list.addToListTail(40)

    list.addToListTail(50)

    list.addToListAtHead(-1)

    println(list.toList().joinToString(", "))
    println(list.toReversedList().joinToString(", "))


    println("---")
    list.removeValue(50)


    println(list.toList().joinToString(", "))
    println(list.toReversedList().joinToString(", "))

    println("---")
    list.addAtIndex(15, 3)
    println(list.toList().joinToString(", "))
    println("get(3) = ${list[3]}, indexOf(15) = ${list.indexOf(15)}, 99 in list = ${99 in list}")

    println("removeFirst = ${list.removeFirst()}, removeLast = ${list.removeLast()}")
    println(list.toList().joinToString(", "))
    println("size = ${list.size}, peekFirst = ${list.peekFirst()}, peekLast = ${list.peekLast()}")

    list.clear()
    println("isEmpty = ${list.isEmpty()}, peekFirst = ${list.peekFirst()}")

}
