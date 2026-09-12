package dsa.mycodeschool.treeOptimised

import dsa.mycodeschool.queue.QueueArray

abstract class AbstractBST {

    protected class Node(var value: Int, var left: Node? = null, var right: Node? = null) {
        var height = 1      // only maintained by AVLTree; plain BST ignores it
    }

    protected var root: Node? = null

    // Called on every node on the way back up from insert/delete, with the
    // possibly-modified subtree. Return the node that should sit in that place.
    // Plain BST: identity. AVL: rebalance.
    protected open fun fixUp(node: Node): Node = node


    private fun insertNode(node: Node?, value: Int): Node {
        if (node == null) {
            return Node(value)
        }

        if (value < node.value) {
            node.left = insertNode(node.left, value)
        } else if (value > node.value) {
            node.right = insertNode(node.right, value)
        } else {
            return node
        }

        return fixUp(node)
    }

    fun insert(value: Int) {
        root = insertNode(root, value)
    }


    private fun findMinNode(node: Node): Node {
        var current = node
        while (true) {
            current = current.left ?: return current
        }
    }

    private fun findMaxNode(node: Node): Node {
        var current = node
        while (true) {
            current = current.right ?: return current
        }
    }

    fun findMin(): Int? = root?.let { findMinNode(it).value }

    fun findMax(): Int? = root?.let { findMaxNode(it).value }


    private fun deleteNode(node: Node?, value: Int): Node? {
        if (node == null) {
            return null
        }

        if (value < node.value) {
            node.left = deleteNode(node.left, value)
        } else if (value > node.value) {
            node.right = deleteNode(node.right, value)
        } else {
            if (node.left == null) {
                return node.right
            }
            if (node.right == null) {
                return node.left
            }

            val successor = findMinNode(node.right!!)
            node.value = successor.value
            node.right = deleteNode(node.right, successor.value)
        }

        return fixUp(node)
    }

    fun delete(value: Int) {
        root = deleteNode(root, value)
    }


    private fun search(node: Node?, value: Int): Boolean {
        if (node == null) {
            return false
        }

        if (node.value == value) {
            return true
        }

        if (value < node.value) {
            return search(node.left, value)
        }

        return search(node.right, value)
    }

    fun searchNode(value: Int): Boolean = search(root, value)

    operator fun contains(value: Int): Boolean = searchNode(value)


    // empty = 0, single node = 1
    open fun height(): Int {
        fun heightRec(node: Node?): Int {
            if (node == null) {
                return 0
            }

            return 1 + maxOf(heightRec(node.left), heightRec(node.right))
        }

        return heightRec(root)
    }


    fun levelOrder(): List<Int> {
        val start = root ?: return emptyList()

        val list = mutableListOf<Int>()
        val queue = QueueArray<Node>(20)
        queue.enqueue(start)

        while (!queue.isEmpty()) {
            val elem = queue.dequeue()
            list.add(elem.value)
            elem.left?.let { queue.enqueue(it) }
            elem.right?.let { queue.enqueue(it) }
        }

        return list
    }


    private enum class Order { PRE, IN, POST }

    private fun dfs(order: Order): List<Int> {
        val list = mutableListOf<Int>()

        fun walk(node: Node?) {
            if (node == null) {
                return
            }

            if (order == Order.PRE) list.add(node.value)
            walk(node.left)
            if (order == Order.IN) list.add(node.value)
            walk(node.right)
            if (order == Order.POST) list.add(node.value)
        }

        walk(root)

        return list
    }

    fun preOrder(): List<Int> = dfs(Order.PRE)

    fun inOrder(): List<Int> = dfs(Order.IN)

    fun postOrder(): List<Int> = dfs(Order.POST)


    fun isBST(): Boolean {
        fun bstRec(node: Node?, minValue: Int?, maxValue: Int?): Boolean {
            if (node == null) {
                return true
            }

            if (minValue != null && node.value <= minValue) {
                return false
            }
            if (maxValue != null && node.value >= maxValue) {
                return false
            }

            return bstRec(node.left, minValue, node.value) && bstRec(node.right, node.value, maxValue)
        }

        return bstRec(root, null, null)
    }
}
