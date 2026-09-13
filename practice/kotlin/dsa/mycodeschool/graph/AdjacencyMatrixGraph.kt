package dsa.mycodeschool.graph

// Vertices are 0 until vertexCount. Unweighted use: just omit the weight (defaults to 1).
// null cell = no edge, so 0 and negative weights are still representable.
// Space O(V^2). hasEdge / weight / addEdge / removeEdge O(1). neighbors O(V).
class AdjacencyMatrixGraph(val vertexCount: Int, val directed: Boolean = false) {

    private val matrix: Array<Array<Int?>> = Array(vertexCount) { arrayOfNulls<Int>(vertexCount) }

    private fun checkVertex(v: Int) {
        require(v in 0 until vertexCount) { "vertex $v out of range 0..${vertexCount - 1}" }
    }

    fun addEdge(u: Int, v: Int, weight: Int = 1) {
        checkVertex(u)
        checkVertex(v)

        matrix[u][v] = weight
        if (!directed) {
            matrix[v][u] = weight
        }
    }

    fun removeEdge(u: Int, v: Int) {
        checkVertex(u)
        checkVertex(v)

        matrix[u][v] = null
        if (!directed) {
            matrix[v][u] = null
        }
    }

    fun hasEdge(u: Int, v: Int): Boolean {
        checkVertex(u)
        checkVertex(v)

        return matrix[u][v] != null
    }

    // null when there is no edge
    fun weight(u: Int, v: Int): Int? {
        checkVertex(u)
        checkVertex(v)

        return matrix[u][v]
    }

    fun neighbors(v: Int): List<Edge> {
        checkVertex(v)

        val result = mutableListOf<Edge>()
        for (u in 0 until vertexCount) {
            val w = matrix[v][u]
            if (w != null) {
                result.add(Edge(u, w))
            }
        }
        return result
    }

    fun edgeCount(): Int {
        var total = 0
        for (row in matrix) {
            total += row.count { it != null }
        }
        return if (directed) total else total / 2
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("   ${(0 until vertexCount).joinToString(" ") { "%2d".format(it) }}\n")
        for (u in 0 until vertexCount) {
            sb.append("$u: ")
            sb.append(matrix[u].joinToString(" ") { if (it == null) " ." else "%2d".format(it) })
            sb.append("\n")
        }
        return sb.toString()
    }
}


fun main() {

    val graph = AdjacencyMatrixGraph(5)

    graph.addEdge(0, 1, 4)
    graph.addEdge(0, 2, 1)
    graph.addEdge(2, 1, 2)
    graph.addEdge(1, 3, 1)
    graph.addEdge(2, 3, 5)
    graph.addEdge(3, 4, 3)

    print(graph)
    println("edges = ${graph.edgeCount()}")
    println("hasEdge(1, 3) = ${graph.hasEdge(1, 3)}, hasEdge(0, 3) = ${graph.hasEdge(0, 3)}")
    println("weight(0, 1) = ${graph.weight(0, 1)}, weight(0, 3) = ${graph.weight(0, 3)}")
    println("neighbors(2) = ${graph.neighbors(2)}")

    graph.removeEdge(2, 3)
    println("after removeEdge(2, 3): neighbors(2) = ${graph.neighbors(2)}, edges = ${graph.edgeCount()}")

    println("--- directed, unweighted (default weight 1)")
    val dag = AdjacencyMatrixGraph(4, directed = true)
    dag.addEdge(0, 1)
    dag.addEdge(0, 2)
    dag.addEdge(1, 3)
    dag.addEdge(2, 3)
    print(dag)
    println("hasEdge(0, 1) = ${dag.hasEdge(0, 1)}, hasEdge(1, 0) = ${dag.hasEdge(1, 0)}")

}
