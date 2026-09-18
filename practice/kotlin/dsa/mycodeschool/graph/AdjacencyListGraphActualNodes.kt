package dsa.mycodeschool.graph

import java.util.PriorityQueue

data class NamedEdge(val to: String, val weight: Int)

data class NamedMSTEdge(val from: String, val to: String, val weight: Int)

// Vertices are plain strings, added on first use. Space O(V + E).
class AdjacencyListGraphActualNodes(val directed: Boolean = false) {

    private val adj = LinkedHashMap<String, MutableList<NamedEdge>>()

    val vertices: Set<String>
        get() = adj.keys

    val vertexCount: Int
        get() = adj.size

    private fun checkVertex(v: String) {
        require(v in adj) { "unknown vertex $v" }
    }

    fun addVertex(v: String) {
        adj.getOrPut(v) { mutableListOf() }
    }

    fun addEdge(u: String, v: String, weight: Int = 1) {
        addVertex(u)
        addVertex(v)

        adj.getValue(u).add(NamedEdge(v, weight))
        if (!directed) {
            adj.getValue(v).add(NamedEdge(u, weight))
        }
    }

    fun removeEdge(u: String, v: String) {
        checkVertex(u)
        checkVertex(v)

        adj.getValue(u).removeAll { it.to == v }
        if (!directed) {
            adj.getValue(v).removeAll { it.to == u }
        }
    }

    fun hasEdge(u: String, v: String): Boolean {
        checkVertex(u)
        checkVertex(v)

        return adj.getValue(u).any { it.to == v }
    }

    fun weight(u: String, v: String): Int? {
        checkVertex(u)
        checkVertex(v)

        return adj.getValue(u).find { it.to == v }?.weight
    }

    fun neighbors(v: String): List<NamedEdge> {
        checkVertex(v)

        return adj.getValue(v)
    }

    fun edgeCount(): Int {
        val total = adj.values.sumOf { it.size }
        return if (directed) total else total / 2
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for ((v, edges) in adj) {
            sb.append("$v -> ${edges.joinToString(", ") { "${it.to}(${it.weight})" }}\n")
        }
        return sb.toString()
    }

    // DisjointSet works on 0 until n, so names get a slot only where it is needed
    private fun vertexIndex(): Map<String, Int> {
        val index = HashMap<String, Int>()
        for (v in adj.keys) {
            index[v] = index.size
        }
        return index
    }

    private fun unreachableDistances(): MutableMap<String, Int> {
        val distance = LinkedHashMap<String, Int>()
        for (v in adj.keys) {
            distance[v] = Int.MAX_VALUE
        }
        return distance
    }

    fun primMST(startNode: String = adj.keys.first()): List<NamedMSTEdge> {
        require(!directed) { "Prim's needs an undirected graph" }
        checkVertex(startNode)

        val visited = HashSet<String>()
        val edgesQueue = PriorityQueue<NamedMSTEdge>(compareBy { it.weight })
        val edgesNeeded = vertexCount - 1
        val mstTreeEdges = mutableListOf<NamedMSTEdge>()

        adj.getValue(startNode).forEach { edgesQueue.add(NamedMSTEdge(startNode, it.to, it.weight)) }
        visited.add(startNode)

        while (edgesQueue.isNotEmpty() && mstTreeEdges.size < edgesNeeded) {
            val minEdge = edgesQueue.poll()
            if (minEdge.to !in visited) {
                mstTreeEdges.add(minEdge)
                visited.add(minEdge.to)

                adj.getValue(minEdge.to).forEach {
                    if (it.to !in visited) {
                        edgesQueue.add(NamedMSTEdge(minEdge.to, it.to, it.weight))
                    }
                }
            }
        }

        check(mstTreeEdges.size == edgesNeeded) { "graph is not connected" }
        return mstTreeEdges
    }

    fun primMSTEager(startNode: String = adj.keys.first()): List<NamedMSTEdge> {
        require(!directed) { "Prim's needs an undirected graph" }
        checkVertex(startNode)

        val inTree = HashSet<String>()
        val bestWeight = HashMap<String, Int>()
        val bestEdge = HashMap<String, NamedMSTEdge>()
        val queue = PriorityQueue<Pair<String, Int>>(compareBy { it.second })
        val mstTreeEdges = mutableListOf<NamedMSTEdge>()

        bestWeight[startNode] = 0
        queue.add(startNode to 0)

        while (queue.isNotEmpty()) {
            val (u, key) = queue.poll()
            if (u in inTree || key > bestWeight.getValue(u)) {
                continue
            }

            inTree.add(u)
            bestEdge[u]?.let { mstTreeEdges.add(it) }

            for (edge in adj.getValue(u)) {
                val v = edge.to
                if (v !in inTree && edge.weight < (bestWeight[v] ?: Int.MAX_VALUE)) {
                    bestWeight[v] = edge.weight
                    bestEdge[v] = NamedMSTEdge(u, v, edge.weight)
                    queue.add(v to edge.weight)
                }
            }
        }

        check(mstTreeEdges.size == vertexCount - 1) { "graph is not connected" }
        return mstTreeEdges
    }

    fun kruskalMST(): List<NamedMSTEdge> {
        require(!directed) { "Kruskal's needs an undirected graph" }

        val allEdges = mutableListOf<NamedMSTEdge>()
        for ((u, edges) in adj) {
            for (edge in edges) {
                if (u < edge.to) {
                    allEdges.add(NamedMSTEdge(u, edge.to, edge.weight))
                }
            }
        }
        allEdges.sortBy { it.weight }

        val index = vertexIndex()
        val components = DisjointSet(vertexCount)
        val edgesNeeded = vertexCount - 1
        val mstTreeEdges = mutableListOf<NamedMSTEdge>()

        for (smallestEdge in allEdges) {
            if (mstTreeEdges.size == edgesNeeded) {
                break
            }

            if (components.union(index.getValue(smallestEdge.from), index.getValue(smallestEdge.to))) {
                mstTreeEdges.add(smallestEdge)
            }
        }

        check(mstTreeEdges.size == edgesNeeded) { "graph is not connected" }
        return mstTreeEdges
    }

    fun findCycleDirected(): List<String>? {
        require(directed) { "use findCycleUndirected for undirected graphs" }

        val onPath = HashSet<String>()
        val done = HashSet<String>()
        val path = mutableListOf<String>()

        fun dfs(u: String): List<String>? {
            onPath.add(u)
            path.add(u)

            for (edge in adj.getValue(u)) {
                if (edge.to in onPath) {
                    val cycleStart = path.indexOf(edge.to)
                    return path.subList(cycleStart, path.size) + edge.to
                }
                if (edge.to !in done) {
                    val cycle = dfs(edge.to)
                    if (cycle != null) {
                        return cycle
                    }
                }
            }

            onPath.remove(u)
            done.add(u)
            path.removeAt(path.lastIndex)
            return null
        }

        for (v in adj.keys) {
            if (v !in done) {
                val cycle = dfs(v)
                if (cycle != null) {
                    return cycle
                }
            }
        }

        return null
    }

    fun hasCycleDirected(): Boolean = findCycleDirected() != null

    fun findCycleUndirected(): List<String>? {
        require(!directed) { "use findCycleDirected for directed graphs" }

        val visited = HashSet<String>()
        val path = mutableListOf<String>()

        fun dfs(u: String, parent: String?): List<String>? {
            visited.add(u)
            path.add(u)
            var parentEdgeSkipped = false

            for (edge in adj.getValue(u)) {
                val v = edge.to

                if (v == parent && !parentEdgeSkipped) {
                    parentEdgeSkipped = true
                    continue
                }
                if (v in visited) {
                    val cycleStart = path.indexOf(v)
                    return path.subList(cycleStart, path.size) + v
                }

                val cycle = dfs(v, u)
                if (cycle != null) {
                    return cycle
                }
            }

            path.removeAt(path.lastIndex)
            return null
        }

        for (v in adj.keys) {
            if (v !in visited) {
                val cycle = dfs(v, null)
                if (cycle != null) {
                    return cycle
                }
            }
        }

        return null
    }

    fun hasCycleUndirected(): Boolean = findCycleUndirected() != null

    fun hasCycleUnionFind(): Boolean {
        require(!directed) { "use findCycleDirected for directed graphs" }

        val index = vertexIndex()
        val components = DisjointSet(vertexCount)

        for ((u, edges) in adj) {
            for (edge in edges) {
                if (u < edge.to) {
                    if (!components.union(index.getValue(u), index.getValue(edge.to))) {
                        return true
                    }
                } else if (u == edge.to) {
                    return true
                }
            }
        }

        return false
    }

    fun getTopologicalSort(): List<String> {
        require(directed) { "Only valid for directed graphs" }

        val inDegree = LinkedHashMap<String, Int>()
        for (v in adj.keys) {
            inDegree[v] = 0
        }
        for (edges in adj.values) {
            for (edge in edges) {
                inDegree[edge.to] = inDegree.getValue(edge.to) + 1
            }
        }

        val queue = ArrayDeque<String>()
        for ((v, degree) in inDegree) {
            if (degree == 0) {
                queue.addLast(v)
            }
        }

        val nodesOrder = mutableListOf<String>()
        while (queue.isNotEmpty()) {
            val u = queue.removeFirst()
            nodesOrder.add(u)

            for (edge in adj.getValue(u)) {
                val remaining = inDegree.getValue(edge.to) - 1
                inDegree[edge.to] = remaining
                if (remaining == 0) {
                    queue.addLast(edge.to)
                }
            }
        }

        check(nodesOrder.size == vertexCount) { "Topological sort not possible" }
        return nodesOrder
    }

    private data class DKNode(val vertex: String, val distance: Int)

    fun dijkstra(source: String): Map<String, Int> {
        checkVertex(source)

        val pq = PriorityQueue<DKNode>(compareBy { it.distance })
        val visited = HashSet<String>()
        val distance = unreachableDistances()

        distance[source] = 0
        pq.add(DKNode(source, 0))

        while (pq.isNotEmpty()) {
            val node = pq.poll()
            if (node.vertex in visited) {
                continue
            }
            visited.add(node.vertex)

            for (edge in adj.getValue(node.vertex)) {
                if (edge.to !in visited) {
                    val newDistance = distance.getValue(node.vertex) + edge.weight
                    if (newDistance < distance.getValue(edge.to)) {
                        distance[edge.to] = newDistance
                        pq.add(DKNode(edge.to, newDistance))
                    }
                }
            }
        }

        return distance
    }

    fun bellmanFord(source: String): Map<String, Int> {
        checkVertex(source)

        val distance = unreachableDistances()
        distance[source] = 0

        fun relaxAllEdges(): Boolean {
            var improved = false
            for ((u, edges) in adj) {
                if (distance.getValue(u) == Int.MAX_VALUE) {
                    continue
                }
                for (edge in edges) {
                    val newDistance = distance.getValue(u) + edge.weight
                    if (newDistance < distance.getValue(edge.to)) {
                        distance[edge.to] = newDistance
                        improved = true
                    }
                }
            }
            return improved
        }

        repeat(vertexCount - 1) {
            if (!relaxAllEdges()) {
                return distance
            }
        }

        check(!relaxAllEdges()) { "graph has a negative cycle reachable from $source" }

        return distance
    }

    fun floydWarshall(): Map<String, Map<String, Int>> {
        val dist = LinkedHashMap<String, MutableMap<String, Int>>()

        for (i in adj.keys) {
            val row = unreachableDistances()
            row[i] = 0
            dist[i] = row
        }
        for ((u, edges) in adj) {
            val row = dist.getValue(u)
            for (edge in edges) {
                row[edge.to] = minOf(row.getValue(edge.to), edge.weight)
            }
        }

        for (k in adj.keys) {
            val rowK = dist.getValue(k)
            for (i in adj.keys) {
                val rowI = dist.getValue(i)
                if (rowI.getValue(k) == Int.MAX_VALUE) {
                    continue
                }
                for (j in adj.keys) {
                    if (rowK.getValue(j) == Int.MAX_VALUE) {
                        continue
                    }
                    val viaK = rowI.getValue(k) + rowK.getValue(j)
                    if (viaK < rowI.getValue(j)) {
                        rowI[j] = viaK
                    }
                }
            }
        }

        for (i in adj.keys) {
            check(dist.getValue(i).getValue(i) >= 0) { "graph has a negative cycle through $i" }
        }

        return dist
    }

    fun dagShortestPath(source: String): Map<String, Int> {
        require(directed) { "Only valid for directed graphs" }
        checkVertex(source)

        val order = getTopologicalSort()
        val distance = unreachableDistances()
        distance[source] = 0

        for (u in order) {
            if (distance.getValue(u) == Int.MAX_VALUE) {
                continue
            }
            for (edge in adj.getValue(u)) {
                val newDistance = distance.getValue(u) + edge.weight
                if (newDistance < distance.getValue(edge.to)) {
                    distance[edge.to] = newDistance
                }
            }
        }

        return distance
    }

    fun findBridgesEdges(): List<NamedMSTEdge> {
        require(!directed) { "Bridges are defined for undirected graphs" }

        val discoveryTime = HashMap<String, Int>()
        val lowestReachable = HashMap<String, Int>()
        val bridgeEdges = mutableListOf<NamedMSTEdge>()

        fun dfs(u: String, parent: String?, currentTime: Int) {
            discoveryTime[u] = currentTime
            lowestReachable[u] = currentTime
            var parentEdgeSkipped = false

            for (edge in adj.getValue(u)) {
                val v = edge.to

                if (v == parent && !parentEdgeSkipped) {
                    parentEdgeSkipped = true
                    continue
                }

                if (v !in discoveryTime) {
                    dfs(v, u, currentTime + 1)

                    lowestReachable[u] = minOf(lowestReachable.getValue(u), lowestReachable.getValue(v))

                    if (lowestReachable.getValue(v) > discoveryTime.getValue(u)) {
                        bridgeEdges.add(NamedMSTEdge(u, v, edge.weight))
                    }
                } else {
                    lowestReachable[u] = minOf(lowestReachable.getValue(u), discoveryTime.getValue(v))
                }
            }
        }

        for (v in adj.keys) {
            if (v !in discoveryTime) {
                dfs(v, null, 0)
            }
        }

        return bridgeEdges
    }
}


fun main() {

    val graph = AdjacencyListGraphActualNodes()

    graph.addEdge("A", "B", 4)
    graph.addEdge("A", "C", 1)
    graph.addEdge("C", "B", 2)
    graph.addEdge("B", "D", 1)
    graph.addEdge("C", "D", 5)
    graph.addEdge("D", "E", 3)

    print(graph)
    println("vertices = ${graph.vertices}, edges = ${graph.edgeCount()}")
    println("hasEdge(B, D) = ${graph.hasEdge("B", "D")}, hasEdge(A, D) = ${graph.hasEdge("A", "D")}")
    println("weight(A, B) = ${graph.weight("A", "B")}, weight(A, D) = ${graph.weight("A", "D")}")
    println("neighbors(C) = ${graph.neighbors("C")}")

    val mst = graph.primMST("A")
    println("primMST = $mst, total weight = ${mst.sumOf { it.weight }}")

    val mstEager = graph.primMSTEager("A")
    println("primMSTEager = $mstEager, total weight = ${mstEager.sumOf { it.weight }}")

    val mstKruskal = graph.kruskalMST()
    println("kruskalMST = $mstKruskal, total weight = ${mstKruskal.sumOf { it.weight }}")

    println("findBridgesEdges = ${graph.findBridgesEdges()}")

    println("dijkstra(A) = ${graph.dijkstra("A")}")
    println("bellmanFord(A) = ${graph.bellmanFord("A")}")

    println("floydWarshall:")
    graph.floydWarshall().forEach { (v, row) -> println("  $v: $row") }

    graph.removeEdge("C", "D")
    println("after removeEdge(C, D): neighbors(C) = ${graph.neighbors("C")}, edges = ${graph.edgeCount()}")
    println("findCycleUndirected = ${graph.findCycleUndirected()}")
    println("findBridgesEdges = ${graph.findBridgesEdges()}")
    println("hasCycleUnionFind = ${graph.hasCycleUnionFind()}")

    println("--- directed, unweighted")
    val courses = AdjacencyListGraphActualNodes(directed = true)
    courses.addEdge("maths", "physics")
    courses.addEdge("maths", "programming")
    courses.addEdge("programming", "ml")
    courses.addEdge("physics", "robotics")
    courses.addEdge("ml", "robotics")
    courses.addVertex("ethics")
    print(courses)
    println("hasEdge(maths, physics) = ${courses.hasEdge("maths", "physics")}, hasEdge(physics, maths) = ${courses.hasEdge("physics", "maths")}")
    println("hasCycleDirected = ${courses.hasCycleDirected()}")
    println("getTopologicalSort = ${courses.getTopologicalSort()}")
    println("dagShortestPath(maths) = ${courses.dagShortestPath("maths")}")

    courses.addEdge("robotics", "maths")
    println("after addEdge(robotics, maths): cycle = ${courses.findCycleDirected()}")
    try {
        courses.getTopologicalSort()
    } catch (e: IllegalStateException) {
        println("getTopologicalSort on cyclic graph: ${e.message}")
    }

    println("--- directed, negative weight")
    val neg = AdjacencyListGraphActualNodes(directed = true)
    neg.addEdge("S", "A", 1)
    neg.addEdge("S", "B", 2)
    neg.addEdge("B", "A", -2)
    neg.addEdge("A", "T", 1)
    println("dijkstra(S)    = ${neg.dijkstra("S")}   <- wrong for A and T")
    println("bellmanFord(S) = ${neg.bellmanFord("S")}")
    println("floydWarshall[S] = ${neg.floydWarshall()["S"]}")
    println("dagShortestPath(S) = ${neg.dagShortestPath("S")}")

    neg.addEdge("A", "B", -1)
    try {
        neg.bellmanFord("S")
    } catch (e: IllegalStateException) {
        println("bellmanFord with negative cycle: ${e.message}")
    }

}
