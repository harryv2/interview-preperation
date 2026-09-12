package dsa.mycodeschool.treeOptimised

private fun demo(name: String, tree: AbstractBST) {
    // sorted input: worst case for a plain BST
    for (value in 1..7) {
        tree.insert(value)
    }

    println("$name: inOrder = ${tree.inOrder()}, levelOrder = ${tree.levelOrder()}, height = ${tree.height()}")

    tree.delete(4)      // root of the AVL tree, two children
    tree.delete(1)
    tree.delete(2)
    tree.delete(3)      // AVL: triggers a rotation on delete

    println("$name: after deletes inOrder = ${tree.inOrder()}, levelOrder = ${tree.levelOrder()}, height = ${tree.height()}, isBST = ${tree.isBST()}")
}

fun main() {
    demo("BST", BST())
    demo("AVL", AVLTree())
}
