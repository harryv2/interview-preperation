package dsa.mycodeschool.tree

import dsa.mycodeschool.queue.QueueArray

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
        return search(treeRoot, value) != null
    }

    operator fun contains(value: Int): Boolean = searchNode(value)


    fun findMin(): Int {
        var current = treeRoot;
        while (current?.left != null) {
            current = current.left
        }

        return current?.value ?: -1
    }

    fun findMax(): Int {
        var current = treeRoot;
        while (current?.right != null) {
            current = current.right
        }

        return current?.value ?: -1
    }


    fun height(): Int {
        fun heightRec(node: Node?): Int {
            if (node == null) {
                return -1
            }

            var left = heightRec(node.left)
            var right = heightRec(node.right)

            return maxOf(left, right) + 1
        }

        return heightRec(treeRoot)
    }


    fun levelOrder(): List<Int> {
        val root = treeRoot ?: return emptyList()

        val list = mutableListOf<Int>()
        val queue = QueueArray<Node>(20)
        queue.enqueue(root)

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

        walk(treeRoot)

        return list
    }

    fun preOrder(): List<Int> = dfs(Order.PRE)

    fun inOrder(): List<Int> = dfs(Order.IN)

    fun postOrder(): List<Int> = dfs(Order.POST)


    fun isBST(): Boolean {

        fun bstRec(root: Node?, minValue: Int, maxValue: Int): Boolean {
            if (root == null) {
                return true
            }

            var rootFine = root.value in (minValue + 1)..maxValue

            var isLeftBst = bstRec(root.left, minValue, root.value)
            var isRightBst = bstRec(root.right, root.value, maxValue)

            return rootFine && isLeftBst && isRightBst

        }

        return bstRec(treeRoot, Int.MIN_VALUE, Int.MAX_VALUE)
    }


    private fun finMinNode(root: Node): Node {
        var current: Node? = root
        while (current?.left != null) {
            current = current.left
        }
        return current!!
    }

    private fun deleteNode(root: Node?, value: Int): Node? {
        if (root == null) {
            return null
        }

        if (value < root.value) {
            root.left = deleteNode(root.left, value)
        } else if (value > root.value) {
            root.right = deleteNode(root.right, value)
        } else {
            //delete
            if (root.left == null && root.right == null) {
                return null
            }

            if (root.left == null) {
                return root.right
            }

            if (root.right == null) {
                return root.left
            }

            var minNode = finMinNode(root.right!!)
            root.value = minNode.value
            root.right = deleteNode(root.right, minNode.value)
            return root
        }

        return root
    }

    fun delete(value: Int) {
        treeRoot = deleteNode(treeRoot, value)
    }


    fun getInorderSuccessor(value: Int): Int {
        val current = search(treeRoot, value) ?: return -1

        if(current.right != null) {
            var successor = finMinNode(current.right!!)
            return successor.value
        } else {
            var ancestor = treeRoot
            var successor: Node? = null;

            while (ancestor != current) {
                if(current.value < ancestor!!.value) {
                    successor = ancestor
                    ancestor = ancestor.left
                } else {
                    ancestor = ancestor.right
                }
            }

            return successor?.value ?: -1
        }
    }

    fun getInorderSuccessor2(value: Int): Int {
        var successor: Node? = null
        var current = treeRoot

        while (current != null && current.value != value) {
            if (value < current.value) {
                successor = current
                current = current.left
            } else {
                current = current.right
            }
        }

        requireNotNull(current) { "Value not found $value" }

        current.right?.let { return finMinNode(it).value }

        return successor?.value ?: -1
    }
}


fun main() {

    val tree = BST()

    tree.insert(15)
    tree.insert(10)
    tree.insert(20)
    tree.insert(25)
    tree.insert(8)
    tree.insert(6)
    tree.insert(11)
    tree.insert(12)


    println(tree.searchNode(12))
    println(tree.searchNode(99))
    println(25 in tree)

    println(tree.findMin())
    println(tree.findMax())

    println("levelOrder:  " + tree.levelOrder().joinToString(", "))
    println("inOrder:  " + tree.inOrder().joinToString(", "))
//    println("preOrder:  " + tree.preOrder().joinToString(", "))
//    println("postOrder:  " + tree.postOrder().joinToString(", "))


    println("isBST: " + tree.isBST())

//    tree.delete(10)
//
//    println("levelOrder:  " + tree.levelOrder().joinToString(", "))
//
//    tree.delete(15)
//
//    println("levelOrder:  " + tree.levelOrder().joinToString(", "))


    println(tree.getInorderSuccessor(8))


}
