package lld_self.cache.lru

class DoublyLinkedList<U, V> {

    class Node<U, V>(var key: U, var value: V, var next: Node<U, V>? = null, var prev: Node<U, V>? = null)

    var head: Node<U, V>? = null
    var tail: Node<U, V>? = null
    var size: Int = 0




    fun addNodeLast(node: Node<U, V>) {
        node.next = null
        node.prev = null
        if (head == null) {
            head = node
            tail = node
        } else {
            tail?.next = node
            node.prev = tail
            tail = node
        }
        size++
    }


    fun addNodeStart(node: Node<U, V>) {
        node.next = null
        node.prev = null
        if (head == null) {
            head = node
            tail = node
        } else {
            head?.prev = node
            node.next = head
            head = node
        }
        size++
    }


    fun removeNode(node: Node<U, V>) {
        val next = node.next
        val prev = node.prev

        prev?.next = next
        next?.prev = prev

        if (node === head) {
            head = next
        }
        if (node === tail) {
            tail = prev
        }

        node.next = null
        node.prev = null
        size--
    }

    fun removeFirst(): Node<U, V> {
        require(size > 0) { "Empty" }
        val node = head!!
        removeNode(node)
        return node
    }

    fun removeLast(): Node<U, V> {
        require(size > 0) { "Empty" }
        val node = tail!!
        removeNode(node)
        return node
    }

}
