package dsa.williamfiset.suffixarray

import java.util.TreeMap

private const val LEAF = Int.MAX_VALUE

class SuffixTree(
    input: String
) {

    private val text = input + "$"
    private val n = text.length

    private inner class Node(
        var start: Int,
        var end: Int
    ) {
        val children = TreeMap<Char, Node>()
        var suffixLink: Node? = null
        var suffixIndex = -1

        fun edgeLength(): Int {
            val actualEnd = if (end == LEAF) leafEnd else end
            return actualEnd - start + 1
        }
    }

    private val root = Node(-1, -1)
    private var leafEnd = -1
    private var activeNode = root
    private var activeEdge = -1
    private var activeLength = 0
    private var remainingSuffixCount = 0
    private var lastNewNode: Node? = null

    val sa: IntArray

    init {
        require(!input.contains('$')) { "input must not contain $" }
        for (i in 0 until n) {
            extend(i)
        }
        setSuffixIndex(root, 0)
        val order = ArrayList<Int>()
        collectLeaves(root, order)
        sa = order.filter { it < n - 1 }.toIntArray()
    }

    private fun extend(pos: Int) {
        leafEnd = pos
        remainingSuffixCount++
        lastNewNode = null

        while (remainingSuffixCount > 0) {
            if (activeLength == 0) {
                activeEdge = pos
            }
            val next = activeNode.children[text[activeEdge]]
            if (next == null) {
                activeNode.children[text[activeEdge]] = Node(pos, LEAF)
                lastNewNode?.suffixLink = activeNode
                lastNewNode = null
            } else {
                if (walkDown(next)) {
                    continue
                }
                if (text[next.start + activeLength] == text[pos]) {
                    if (lastNewNode != null && activeNode != root) {
                        lastNewNode?.suffixLink = activeNode
                        lastNewNode = null
                    }
                    activeLength++
                    break
                }
                val split = Node(next.start, next.start + activeLength - 1)
                activeNode.children[text[activeEdge]] = split
                split.children[text[pos]] = Node(pos, LEAF)
                next.start += activeLength
                split.children[text[next.start]] = next
                lastNewNode?.suffixLink = split
                lastNewNode = split
            }

            remainingSuffixCount--
            if (activeNode == root && activeLength > 0) {
                activeLength--
                activeEdge = pos - remainingSuffixCount + 1
            } else if (activeNode != root) {
                activeNode = activeNode.suffixLink ?: root
            }
        }
    }

    private fun walkDown(node: Node): Boolean {
        val length = node.edgeLength()
        if (activeLength >= length) {
            activeEdge += length
            activeLength -= length
            activeNode = node
            return true
        }
        return false
    }

    private fun setSuffixIndex(node: Node, labelHeight: Int) {
        if (node.children.isEmpty()) {
            node.suffixIndex = n - labelHeight
            return
        }
        for (child in node.children.values) {
            setSuffixIndex(child, labelHeight + child.edgeLength())
        }
    }

    private fun collectLeaves(node: Node, out: MutableList<Int>) {
        if (node.children.isEmpty()) {
            out.add(node.suffixIndex)
            return
        }
        for (child in node.children.values) {
            collectLeaves(child, out)
        }
    }
}


fun main() {

    val text = "ABBABAABAA"

    println(SuffixTree(text).sa.toList())   // [9, 8, 5, 6, 3, 0, 7, 4, 2, 1]

}
