package lld.games.leaderboard.structure

import kotlin.random.Random


// a size augmented randomised BST. Every node knows how many nodes sit under it, and that one extra field is
// what turns rank and select into O(log n) descents. It is the same idea as the span field Redis hangs on every
// forward pointer of a skiplist node, with less bookkeeping to get wrong.
class OrderStatisticTree<T>(
    private val comparator: Comparator<T>,
    seed: Int = 20260925
) : RankedSet<T> {

    private class Node<T>(val value: T, val priority: Int) {
        var left: Node<T>? = null
        var right: Node<T>? = null
        var subtreeSize: Int = 1
    }

    private val random = Random(seed)
    private var root: Node<T>? = null

    override val size: Int
        get() = sizeOf(root)

    override fun insert(value: T) {
        val (less, rest) = splitLess(root, value)
        val (_, greater) = splitLessOrEqual(rest, value)
        root = merge(merge(less, Node(value, random.nextInt())), greater)
    }

    override fun remove(value: T): Boolean {
        val (less, rest) = splitLess(root, value)
        val (equal, greater) = splitLessOrEqual(rest, value)
        root = merge(less, greater)
        return equal != null
    }

    override fun rankOf(value: T): Int {
        var node = root
        var passed = 0

        while (node != null) {
            val direction = comparator.compare(value, node.value)
            node = when {
                direction < 0 -> node.left
                direction > 0 -> {
                    passed += sizeOf(node.left) + 1
                    node.right
                }
                else -> return passed + sizeOf(node.left)
            }
        }
        return -1
    }

    override fun select(index: Int): T? {
        if (index < 0 || index >= size) {
            return null
        }

        var node = root
        var remaining = index

        while (node != null) {
            val leftSize = sizeOf(node.left)
            when {
                remaining < leftSize -> node = node.left
                remaining == leftSize -> return node.value
                else -> {
                    remaining -= leftSize + 1
                    node = node.right
                }
            }
        }
        return null
    }

    override fun range(from: Int, count: Int): List<T> {
        if (from < 0 || count <= 0) {
            return emptyList()
        }

        val collected = mutableListOf<T>()
        collectFrom(root, from, count, collected)
        return collected
    }

    // descends only into the subtrees the window actually touches, O(log n + count) rather than a select per slot
    private fun collectFrom(node: Node<T>?, skip: Int, count: Int, into: MutableList<T>) {
        if (node == null || into.size >= count) {
            return
        }

        val leftSize = sizeOf(node.left)

        if (skip < leftSize) {
            collectFrom(node.left, skip, count, into)
        }
        if (into.size >= count) {
            return
        }

        if (skip <= leftSize) {
            into.add(node.value)
        }
        if (into.size >= count) {
            return
        }

        val skipOnRight = if (skip > leftSize + 1) skip - leftSize - 1 else 0
        collectFrom(node.right, skipOnRight, count, into)
    }

    private fun merge(left: Node<T>?, right: Node<T>?): Node<T>? {
        if (left == null) {
            return right
        }
        if (right == null) {
            return left
        }

        return if (left.priority > right.priority) {
            left.right = merge(left.right, right)
            pull(left)
            left
        } else {
            right.left = merge(left, right.left)
            pull(right)
            right
        }
    }

    private fun splitLess(node: Node<T>?, value: T): Pair<Node<T>?, Node<T>?> {
        if (node == null) {
            return Pair(null, null)
        }

        return if (comparator.compare(node.value, value) < 0) {
            val (mid, right) = splitLess(node.right, value)
            node.right = mid
            pull(node)
            Pair(node, right)
        } else {
            val (left, mid) = splitLess(node.left, value)
            node.left = mid
            pull(node)
            Pair(left, node)
        }
    }

    private fun splitLessOrEqual(node: Node<T>?, value: T): Pair<Node<T>?, Node<T>?> {
        if (node == null) {
            return Pair(null, null)
        }

        return if (comparator.compare(node.value, value) <= 0) {
            val (mid, right) = splitLessOrEqual(node.right, value)
            node.right = mid
            pull(node)
            Pair(node, right)
        } else {
            val (left, mid) = splitLessOrEqual(node.left, value)
            node.left = mid
            pull(node)
            Pair(left, node)
        }
    }

    private fun pull(node: Node<T>) {
        node.subtreeSize = 1 + sizeOf(node.left) + sizeOf(node.right)
    }

    private fun sizeOf(node: Node<T>?): Int {
        return node?.subtreeSize ?: 0
    }
}
