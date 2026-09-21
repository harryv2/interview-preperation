package dsa.mycodeschool.unionfind


class UnionFind(
    val n: Int
) {

    private val parents = IntArray(n) { it }
    private var size = IntArray(n) { 1 }

    private var numOfComponents = n


    private fun findParent(x: Int): Int {
        val parent = parents[x]
        if (parent != x) {
            parents[x] = findParent(parent)
        }
        return parents[x]
    }

    fun isConnected(x: Int, y: Int) = findParent(x) == findParent(y)

    fun numOfGroup(): Int = numOfComponents

    /**
     * Merges two elements into the same group, if they are not already in the same group.
     *
     * @param x The first element to be united.
     * @param y The second element to be united.
     * @return `true` if the union operation merged two previously separate groups,
     *         `false` if the elements were already in the same group.
     */
    fun union(x: Int, y: Int): Boolean {
        val p1 = findParent(x)
        val p2 = findParent(y)

        if (p1 == p2) {
            return false
        }

        if (size[p1] >= size[p2]) {
            parents[p2] = p1
            size[p1] += size[p2]
            size[p2] = 0
        } else {
            parents[p1] = p2
            size[p2] += size[p1]
            size[p1] = 0
        }

        numOfComponents--

        return true
    }


    fun groupSize(x: Int): Int {
        return size[findParent(x)]
    }
}


fun main() {

    val ds = UnionFind(5)

    println(ds.union(0, 1))          // true
    println(ds.union(2, 3))          // true
    println(ds.isConnected(1, 2))      // false, {0,1} and {2,3} are separate
    println(ds.union(1, 2))          // true, merges them
    println(ds.isConnected(0, 3))      // true
    println(ds.union(0, 3))          // false, already connected

}