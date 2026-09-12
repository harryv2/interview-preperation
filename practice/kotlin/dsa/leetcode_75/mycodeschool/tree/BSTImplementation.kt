package dsa.leetcode_75.mycodeschool.tree

class BST {


    private class Node(var value: Int, var left: Node? = null, var right: Node? = null)


    private var treeRoot: Node? = null

    private fun insertNode(node: Node?, value: Int): Node {
        if (node == null) {
            return Node(value)
        }

        if (value < node.value) {
            node.left = insertNode(node.left, value)
        } else if (value > node.value) {
            node.right = insertNode(node.right, value)
        }

        return node
    }

    fun insert(value: Int) {
        treeRoot = insertNode(treeRoot, value)
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

    fun searchNode(value: Int): Boolean {
        return search(treeRoot, value)
    }

    operator fun contains(value: Int): Boolean = searchNode(value)
}


fun main() {

    val tree = BST()

    tree.insert(15)
    tree.insert(10)
    tree.insert(20)
    tree.insert(25)
    tree.insert(8)
    tree.insert(12)


    println(tree.searchNode(12))
    println(tree.searchNode(99))
    println(25 in tree)

}
