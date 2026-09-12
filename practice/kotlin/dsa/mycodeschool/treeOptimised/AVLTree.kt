package dsa.mycodeschool.treeOptimised

class AVLTree : AbstractBST() {

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

    // Runs on every ancestor on the way back up from insert AND delete.
    override fun fixUp(node: Node): Node = rebalance(node)

    // O(1) instead of the base class's O(n) walk, since every node caches its height.
    override fun height(): Int = height(root)
}
