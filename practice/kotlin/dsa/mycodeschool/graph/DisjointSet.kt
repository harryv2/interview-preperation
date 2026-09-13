package dsa.mycodeschool.graph

// Union-find over elements 0 until size, with path compression and union by rank.
// find / union are amortised O(α(n)) — effectively constant.
class DisjointSet(size: Int) {

    private val parent = IntArray(size) { it }
    private val rank = IntArray(size) { 0 }

    fun find(x: Int): Int {
        if (parent[x] != x) {
            parent[x] = find(parent[x])
        }
        return parent[x]
    }

    // false if x and y were already in the same set
    fun union(x: Int, y: Int): Boolean {
        val rootX = find(x)
        val rootY = find(y)
        if (rootX == rootY) {
            return false
        }

        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY
        } else if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX
        } else {
            parent[rootY] = rootX
            rank[rootX]++
        }
        return true
    }

    fun connected(x: Int, y: Int): Boolean = find(x) == find(y)
}


fun main() {

    val ds = DisjointSet(5)

    println(ds.union(0, 1))          // true
    println(ds.union(2, 3))          // true
    println(ds.connected(1, 2))      // false, {0,1} and {2,3} are separate
    println(ds.union(1, 2))          // true, merges them
    println(ds.connected(0, 3))      // true
    println(ds.union(0, 3))          // false, already connected

}
