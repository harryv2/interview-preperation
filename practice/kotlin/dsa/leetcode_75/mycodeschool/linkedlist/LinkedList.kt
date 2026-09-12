package dsa.leetcode_75.mycodeschool.linkedlist

class LinkedList<T> {

    private class LinkedListNode<T>(val value: T, var next: LinkedListNode<T>? = null)

    private var head: LinkedListNode<T>? = null
    private var tail: LinkedListNode<T>? = null

    var size: Int = 0
        private set

    fun isEmpty() = size == 0

    fun getHeadValue(): T {
        val first = checkNotNull(head) { "List is empty" }
        return first.value
    }

    fun getTailValue(): T {
        val last = checkNotNull(tail) { "List is empty" }
        return last.value
    }


    fun addInFirst(value: T) {
        val node = LinkedListNode(value, next = head)
        head = node
        if (tail == null) {
            tail = node
        }

        size++
    }

    fun addInLast(value: T) {
        val node = LinkedListNode(value)
        tail?.next = node
        tail = node
        if (head == null) {
            head = node
        }

        size++
    }

    fun addInIndex(value: T, index: Int) {
        require(index in 0..size) { "index $index out of bounds" }

        if (index == 0) {
            addInFirst(value)
            return
        }
        if (index == size) {
            addInLast(value)
            return
        }

        val prevPtr = nodeAt(index - 1)
        prevPtr.next = LinkedListNode(value, next = prevPtr.next)

        size++
    }

    private fun nodeAt(index: Int): LinkedListNode<T> {
        require(index in 0 until size) { "index $index out of bounds" }

        var currentPtr = head!!
        repeat(index) { currentPtr = currentPtr.next!! }
        return currentPtr
    }

    operator fun get(index: Int): T = nodeAt(index).value

    private fun unlink(node: LinkedListNode<T>, prevNode: LinkedListNode<T>?): T {
        if (prevNode == null) {
            head = node.next
        } else {
            prevNode.next = node.next
        }
        if (node === tail) {
            tail = prevNode
        }

        size--
        return node.value
    }

    fun removeAtIndex(index: Int): T {
        require(index in 0 until size) { "index $index out of bounds" }

        val prevPtr = if (index == 0) null else nodeAt(index - 1)
        val currentPtr = if (prevPtr == null) head!! else prevPtr.next!!
        return unlink(currentPtr, prevPtr)
    }

    fun deleteNodeFromList(index: Int): T = removeAtIndex(index)

    fun removeFirst(): T {
        val first = checkNotNull(head) { "List is empty" }
        return unlink(first, null)
    }

    fun indexOf(value: T): Int {
        var i = 0
        var currentPtr = head
        while (currentPtr != null) {
            if (currentPtr.value == value) {
                return i
            }
            currentPtr = currentPtr.next
            i++
        }

        return -1
    }

    operator fun contains(value: T) = indexOf(value) != -1

    fun removeFromList(value: T): T {
        var prevPtr: LinkedListNode<T>? = null
        var currentPtr = head
        while (currentPtr != null && currentPtr.value != value) {
            prevPtr = currentPtr
            currentPtr = currentPtr.next
        }

        requireNotNull(currentPtr) { "Value not found $value" }
        return unlink(currentPtr, prevPtr)
    }

    fun clear() {
        head = null
        tail = null
        size = 0
    }


    fun toList(): List<T> {
        val list = ArrayList<T>(size)

        var currentPtr = head
        while (currentPtr != null) {
            list.add(currentPtr.value)
            currentPtr = currentPtr.next
        }
        return list
    }



    fun reverseList() {
        tail = head

        var current = head
        var prev: LinkedListNode<T>? = null

        while (current != null) {
            val next = current.next
            current.next = prev
            prev = current
            current = next
        }

        head = prev
    }



    fun printRecursion() {
        println("START")

        fun printNode(node: LinkedListNode<T>?) {
            if (node == null) {
                return
            }

            println(node.value)
            printNode(node.next)
        }

        printNode(head)

        println("END")
    }


    fun printListReverseRecursion() {
        println("REV START")


        fun printNode(node: LinkedListNode<T>?) {
            if (node == null) {
                return
            }

            printNode(node.next)
            println(node.value)
        }

        printNode(head)

        println("REV END")

    }


    fun reverseListRecursion() {
        tailrec fun reverseListR(node: LinkedListNode<T>?, prevNode: LinkedListNode<T>?): LinkedListNode<T>? {
            if (node == null) {
                return prevNode
            }

            val next = node.next
            node.next = prevNode
            return reverseListR(next, node)
        }

        tail = head
        head = reverseListR(head, null)
    }

    fun reverseListRecursion2() {
        fun reverseListR(node: LinkedListNode<T>?) {
            if (node?.next == null) {
                head = node
                return
            }

            reverseListR(node.next)
            node.next!!.next = node
            node.next = null
        }

        tail = head
        reverseListR(head)
    }
}


fun main() {

    val list = LinkedList<Int>()

    list.addInLast(10)

    list.addInLast(20)

    list.addInLast(30)

    list.addInLast(40)

    list.addInLast(45)

    list.addInIndex(25, 2)

    list.addInIndex(5, 0)

    println(list.toList().joinToString(", "))
//
//    list.removeFromList(5)
//
//    println(list.toList().joinToString(", "))
//
//    list.deleteNodeFromList(5)
//
//    println(list.toList().joinToString(", "))

//    list.reverseList()

//    println(list.toList().joinToString(", "))

//    list.printRecursion()

//    list.printListReverseRecursion()

    list.reverseListRecursion2()

    println(list.toList().joinToString(", "))

    println("head = ${list.getHeadValue()}, tail = ${list.getTailValue()}, size = ${list.size}")
    println("get(2) = ${list[2]}, indexOf(25) = ${list.indexOf(25)}, 99 in list = ${99 in list}")

    list.addInLast(1)
    println("after addInLast(1): tail = ${list.getTailValue()}, list = ${list.toList().joinToString(", ")}")

}
