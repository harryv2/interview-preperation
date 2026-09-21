package dsa.leetcode_75

import kotlin.collections.ArrayDeque


class ListNode(var `val`: Int) {
    var next: ListNode? = null

    // Secondary constructor to easily initialize a list from an array
    constructor(array: IntArray) : this(array[0]) {
        var current = this
        for (i in 1 until array.size) {
            current.next = ListNode(array[i])
            current = current.next!!
        }
    }

    override fun toString(): String {
        val address = Integer.toHexString(System.identityHashCode(this))
        return "ListNode(val=${`val`}, address=@$address)"
    }

    fun printList(): String {
        val result = mutableListOf<Int>()
        val visited = hashSetOf<ListNode>()
        var current: ListNode? = this

        while (current != null) {
            if (visited.contains(current)) {
                result.add(current.`val`) // Optional: show the start of the loop
                return result.joinToString(" -> ") + " -> (Cycle detected!)"
            }
            visited.add(current)
            result.add(current.`val`)
            current = current.next
        }
        return result.joinToString(" -> ")
    }


}



// 1. The exact LeetCode class
class TreeNode(var `val`: Int) {
    var left: TreeNode? = null
    var right: TreeNode? = null
}

// 2. Helper to build trees from LeetCode's array format (Level-Order)
fun buildTree(values: Array<Int?>): TreeNode? {
    if (values.isEmpty() || values[0] == null) return null

    val root = TreeNode(values[0]!!)
    val queue = ArrayDeque<TreeNode>()
    queue.addLast(root)

    var i = 1
    while (i < values.size) {
        val current = queue.removeFirst()

        // Process left child
        if (i < values.size && values[i] != null) {
            current.left = TreeNode(values[i]!!)
            queue.addLast(current.left!!)
        }
        i++

        // Process right child
        if (i < values.size && values[i] != null) {
            current.right = TreeNode(values[i]!!)
            queue.addLast(current.right!!)
        }
        i++
    }

    return root
}



// Helper to find a node by value within the tree
fun findNode(root: TreeNode?, value: Int): TreeNode? {
    if (root == null) return null
    if (root.`val` == value) return root

    val leftSearch = findNode(root.left, value)
    if (leftSearch != null) return leftSearch

    return findNode(root.right, value)
}