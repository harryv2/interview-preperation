package lld_self.cache.lru

fun <U, V> DoublyLinkedList<U, V>.forward(): List<U> {
    val keys = mutableListOf<U>()
    var node = head
    while (node != null) {
        keys.add(node.key)
        node = node.next
    }
    return keys
}

fun <U, V> DoublyLinkedList<U, V>.backward(): List<U> {
    val keys = mutableListOf<U>()
    var node = tail
    while (node != null) {
        keys.add(node.key)
        node = node.prev
    }
    return keys
}

fun <U, V> DoublyLinkedList<U, V>.check(expected: List<U>) {
    check(forward() == expected) { "forward ${forward()} != $expected" }
    check(backward() == expected.reversed()) { "backward ${backward()} != ${expected.reversed()}" }
    check(size == expected.size) { "size $size != ${expected.size}" }
    check(head?.prev == null && tail?.next == null) { "dangling head.prev or tail.next" }
    println("list ok: $expected")
}

fun <T, U> LRUCache<T, U>.check(expected: List<T>) {
    check(keys() == expected) { "order ${keys()} != $expected" }
    check(size == expected.size) { "size $size != ${expected.size}" }
    check(size <= maxSize) { "size $size exceeds maxSize $maxSize" }
    for (key in expected) {
        check(contains(key)) { "missing $key" }
    }
    println("cache ok: $expected")
}

fun testDoublyLinkedList() {
    val list = DoublyLinkedList<Int, Int>()
    val nodes = (1..5).map { DoublyLinkedList.Node(it, it) }

    nodes.forEach { list.addNodeLast(it) }
    list.check(listOf(1, 2, 3, 4, 5))

    list.removeNode(nodes[2])
    list.check(listOf(1, 2, 4, 5))

    list.addNodeLast(nodes[2])
    list.check(listOf(1, 2, 4, 5, 3))

    check(list.removeFirst() === nodes[0])
    list.check(listOf(2, 4, 5, 3))

    check(list.removeLast() === nodes[2])
    list.check(listOf(2, 4, 5))

    list.addNodeStart(nodes[0])
    list.check(listOf(1, 2, 4, 5))

    list.removeNode(nodes[0])
    list.removeNode(nodes[4])
    list.check(listOf(2, 4))

    list.removeNode(nodes[1])
    list.removeNode(nodes[3])
    list.check(emptyList())
    check(list.head == null && list.tail == null)

    list.addNodeStart(nodes[0])
    list.check(listOf(1))
}

fun testLRUCache() {
    val cache = LRUCache<Int, String>(3)

    cache.add(1, "one")
    cache.add(2, "two")
    cache.add(3, "three")
    cache.check(listOf(1, 2, 3))

    check(cache.get(1) == "one")
    cache.check(listOf(2, 3, 1))

    cache.add(4, "four")
    cache.check(listOf(3, 1, 4))
    check(cache.get(2) == null)
    check(!cache.contains(2))

    cache.add(3, "THREE")
    cache.check(listOf(1, 4, 3))
    check(cache.get(3) == "THREE")

    cache.add(5, "five")
    cache.check(listOf(4, 3, 5))
    check(cache.get(1) == null)

    check(cache.get(4) == "four")
    cache.check(listOf(3, 5, 4))

    check(cache.get(3) == "THREE")
    cache.check(listOf(5, 4, 3))

    check(cache.get(5) == "five")
    cache.check(listOf(4, 3, 5))

    cache.add(6, "six")
    cache.add(7, "seven")
    cache.add(8, "eight")
    cache.check(listOf(6, 7, 8))

    val single = LRUCache<String, Int>(1)
    single.add("a", 1)
    single.add("b", 2)
    single.check(listOf("b"))
    check(single.get("a") == null)
    check(single.get("b") == 2)
    single.add("b", 3)
    single.check(listOf("b"))
    check(single.get("b") == 3)

    val big = LRUCache<Int, Int>(100)
    for (i in 0 until 1000) {
        big.add(i, i * i)
    }
    big.check((900 until 1000).toList())
    for (i in 900 until 1000) {
        check(big.get(i) == i * i)
    }
    big.check((900 until 1000).toList())
    for (i in 0 until 900) {
        check(!big.contains(i))
    }
}

fun main() {
    testDoublyLinkedList()
    testLRUCache()
    println("all checks passed")
}
