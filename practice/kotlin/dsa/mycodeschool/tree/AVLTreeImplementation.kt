package dsa.mycodeschool.tree

class AVLTree {


    private class Node(var value: Int, var left: Node? = null, var right: Node? = null) {
        var height = 1
    }


    private var root: Node? = null

    private fun height(node: Node?): Int = node?.height ?: 0

    private fun updateHeight(node: Node) {
        node.height = 1 + maxOf(height(node.left), height(node.right))
    }

    private fun balanceFactor(node: Node): Int = height(node.left) - height(node.right)

    //       y                x
    //      / \              / \
    //     x   C    ->      A   y
    //    / \                  / \
    //   A   B                B   C
    private fun rotateRight(y: Node): Node {
        val x = y.left!!
        y.left = x.right
        x.right = y
        updateHeight(y)
        updateHeight(x)
        return x
    }

    //     x                  y
    //    / \                / \
    //   A   y      ->      x   C
    //      / \            / \
    //     B   C          A   B
    private fun rotateLeft(x: Node): Node {
        val y = x.right!!
        x.right = y.left
        y.left = x
        updateHeight(x)
        updateHeight(y)
        return y
    }

    private fun rebalance(node: Node): Node {
        updateHeight(node)
        val balance = balanceFactor(node)

        if (balance > 1) {
            if (balanceFactor(node.left!!) < 0) {
                node.left = rotateLeft(node.left!!)
            }
            return rotateRight(node)
        }

        if (balance < -1) {
            if (balanceFactor(node.right!!) > 0) {
                node.right = rotateRight(node.right!!)
            }
            return rotateLeft(node)
        }

        return node
    }

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

        return rebalance(node)
    }

    fun insert(value: Int) {
        root = insertNode(root, value)
    }

    private fun search(node: Node?, value: Int): Node? {
        if (node == null) {
            return null
        }

        if (node.value == value) {
            return node
        }

        if (value < node.value) {
            return search(node.left, value)
        }

        return search(node.right, value)
    }

    fun searchNode(value: Int): Boolean {
        return search(root, value) != null
    }

    operator fun contains(value: Int): Boolean = searchNode(value)

    fun height(): Int = height(root)

    fun toList(): List<Int> {
        val list = mutableListOf<Int>()

        fun inorder(node: Node?) {
            if (node == null) {
                return
            }

            inorder(node.left)
            list.add(node.value)
            inorder(node.right)
        }

        inorder(root)
        return list
    }
}


fun main() {

    val tree = AVLTree()

    // sorted input: a plain BST would become a linked list of height 7
    for (value in 1..7) {
        tree.insert(value)
    }

    println(tree.toList().joinToString(", "))
    println("height = ${tree.height()}")

    println(tree.searchNode(5))
    println(99 in tree)

}
