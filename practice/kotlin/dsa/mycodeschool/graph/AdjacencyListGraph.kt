package dsa.mycodeschool.graph

import java.util.PriorityQueue

// Shared by both graph representations.
data class Edge(val to: Int, val weight: Int)


data class MSTEdge(val from: Int, val to: Int, val weight: Int)

// Vertices are 0 until vertexCount. Unweighted use: just omit the weight (defaults to 1).
// Space O(V + E). hasEdge / weight / removeEdge O(degree). neighbors O(1).
class AdjacencyListGraph(val vertexCount: Int, val directed: Boolean = false) {

    private val adj: Array<MutableList<Edge>> = Array(vertexCount) { mutableListOf() }

    private fun checkVertex(v: Int) {
        require(v in 0 until vertexCount) { "vertex $v out of range 0..${vertexCount - 1}" }
    }

    fun addEdge(u: Int, v: Int, weight: Int = 1) {
        checkVertex(u)
        checkVertex(v)

        adj[u].add(Edge(v, weight))
        if (!directed) {
            adj[v].add(Edge(u, weight))
        }
    }

    fun removeEdge(u: Int, v: Int) {
        checkVertex(u)
        checkVertex(v)

        adj[u].removeAll { it.to == v }
        if (!directed) {
            adj[v].removeAll { it.to == u }
        }
    }

    fun hasEdge(u: Int, v: Int): Boolean {
        checkVertex(u)
        checkVertex(v)

        return adj[u].any { it.to == v }
    }

    // null when there is no edge
    fun weight(u: Int, v: Int): Int? {
        checkVertex(u)
        checkVertex(v)

        return adj[u].find { it.to == v }?.weight
    }

    fun neighbors(v: Int): List<Edge> {
        checkVertex(v)

        return adj[v]
    }

    fun edgeCount(): Int {
        val total = adj.sumOf { it.size }
        return if (directed) total else total / 2
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (v in 0 until vertexCount) {
            sb.append("$v -> ${adj[v].joinToString(", ") { "${it.to}(${it.weight})" }}\n")
        }
        return sb.toString()
    }

    fun primMST(startNode: Int = 0): List<MSTEdge> {
        require(!directed) { "Prim's needs an undirected graph" }
        checkVertex(startNode)

        val visited = IntArray(vertexCount) { 0 }
        val edgesQueue = PriorityQueue<MSTEdge>(compareBy { it.weight })
        val edgesNeeded = vertexCount - 1
        val mstTreeEdges = mutableListOf<MSTEdge>()

        val startEdges = adj[startNode]
        startEdges.forEach { edgesQueue.add(MSTEdge(startNode, it.to, it.weight)) }
        visited[startNode] = 1

        while (edgesQueue.isNotEmpty() && mstTreeEdges.size < edgesNeeded) {
            val minEdge = edgesQueue.poll()
            if (visited[minEdge.to] == 0) {
                mstTreeEdges.add(minEdge)
                visited[minEdge.to] = 1

                val moreEdges = adj[minEdge.to]
                moreEdges.forEach {
                    if (visited[it.to] == 0) {
                        edgesQueue.add(MSTEdge(minEdge.to, it.to, it.weight))
                    }
                }
            }
        }

        check(mstTreeEdges.size == edgesNeeded) { "graph is not connected" }
        return mstTreeEdges
    }

    // Eager Prim: the queue holds vertices keyed by the cheapest known edge into the tree
    // (same idea as Dijkstra). java.util.PriorityQueue has no decreaseKey, so a better key
    // is re-inserted and the older entry is skipped as stale when it pops.
    fun primMSTEager(startNode: Int = 0): List<MSTEdge> {
        require(!directed) { "Prim's needs an undirected graph" }
        checkVertex(startNode)

        val inTree = IntArray(vertexCount) { 0 }
        val bestWeight = IntArray(vertexCount) { Int.MAX_VALUE }
        val bestEdge = arrayOfNulls<MSTEdge>(vertexCount)
        val queue = PriorityQueue<Pair<Int, Int>>(compareBy { it.second })   // (vertex, key)
        val mstTreeEdges = mutableListOf<MSTEdge>()

        bestWeight[startNode] = 0
        queue.add(startNode to 0)

        while (queue.isNotEmpty()) {
            val (u, key) = queue.poll()
            if (inTree[u] == 1 || key > bestWeight[u]) {
                continue
            }

            inTree[u] = 1
            bestEdge[u]?.let { mstTreeEdges.add(it) }

            for (edge in adj[u]) {
                val v = edge.to
                if (inTree[v] == 0 && edge.weight < bestWeight[v]) {
                    bestWeight[v] = edge.weight
                    bestEdge[v] = MSTEdge(u, v, edge.weight)
                    queue.add(v to edge.weight)
                }
            }
        }

        check(mstTreeEdges.size == vertexCount - 1) { "graph is not connected" }
        return mstTreeEdges
    }


    fun kruskalMST(): List<MSTEdge> {
        require(!directed) { "Kruskal's needs an undirected graph" }

        val allEdges = mutableListOf<MSTEdge>()

        for (u in 0 until vertexCount) {
            for (edge in adj[u]) {
                if (u < edge.to) {                        // each undirected edge once
                    allEdges.add(MSTEdge(u, edge.to, edge.weight))
                }
            }
        }

        allEdges.sortBy { it.weight }

        val components = DisjointSet(vertexCount)
        val edgesNeeded = vertexCount - 1
        val mstTreeEdges = mutableListOf<MSTEdge>()

        for (smallestEdge in allEdges) {
            if (mstTreeEdges.size == edgesNeeded) {
                break
            }

            // union is false when from and to are already in the same tree -> cycle
            if (components.union(smallestEdge.from, smallestEdge.to)) {
                mstTreeEdges.add(smallestEdge)
            }
        }

        check(mstTreeEdges.size == edgesNeeded) { "graph is not connected" }
        return mstTreeEdges
    }


    // DFS with three states: 0 = not visited, 1 = on the current DFS path, 2 = fully explored.
    // Reaching a vertex that is on the current path (state 1) means a back edge -> cycle.
    // Returns the cycle as a closed walk, e.g. [0, 1, 3, 0], or null if the graph is acyclic.
    fun findCycleDirected(): List<Int>? {
        require(directed) { "use DisjointSet-based check for undirected graphs" }

        val state = IntArray(vertexCount) { 0 }
        val path = mutableListOf<Int>()          // current DFS path, root .. u

        fun dfs(u: Int): List<Int>? {
            state[u] = 1
            path.add(u)

            for (edge in adj[u]) {
                if (state[edge.to] == 1) {
                    // back edge u -> edge.to: the cycle is the path from edge.to down to u
                    val cycleStart = path.indexOf(edge.to)
                    return path.subList(cycleStart, path.size) + edge.to
                }
                if (state[edge.to] == 0) {
                    val cycle = dfs(edge.to)
                    if (cycle != null) {
                        return cycle
                    }
                }
            }

            state[u] = 2
            path.removeAt(path.lastIndex)
            return null
        }

        for (v in 0 until vertexCount) {
            if (state[v] == 0) {
                val cycle = dfs(v)
                if (cycle != null) {
                    return cycle
                }
            }
        }

        return null
    }

    fun hasCycleDirected(): Boolean = findCycleDirected() != null


    // Undirected: two states are enough. A visited neighbour is always an ancestor on the
    // current path (there are no cross edges in an undirected DFS), so it is a cycle -- unless
    // it is the edge we just arrived by. That parent edge is skipped exactly once, so a
    // second parallel edge to the parent still counts as a cycle.
    fun findCycleUndirected(): List<Int>? {
        require(!directed) { "use findCycleDirected for directed graphs" }

        val visited = BooleanArray(vertexCount)
        val path = mutableListOf<Int>()

        fun dfs(u: Int, parent: Int): List<Int>? {
            visited[u] = true
            path.add(u)
            var parentEdgeSkipped = false

            for (edge in adj[u]) {
                val v = edge.to

                if (v == parent && !parentEdgeSkipped) {
                    parentEdgeSkipped = true
                    continue
                }
                if (visited[v]) {
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

        for (v in 0 until vertexCount) {
            if (!visited[v]) {
                val cycle = dfs(v, -1)
                if (cycle != null) {
                    return cycle
                }
            }
        }

        return null
    }

    fun hasCycleUndirected(): Boolean = findCycleUndirected() != null


    // Undirected, no DFS: union every edge; the first edge whose endpoints are already in
    // the same set closes a cycle. O(E * α(V)). Cannot report the cycle's vertices,
    // only the edge that closed it.
    fun hasCycleUnionFind(): Boolean {
        require(!directed) { "use findCycleDirected for directed graphs" }

        val components = DisjointSet(vertexCount)

        for (u in 0 until vertexCount) {
            for (edge in adj[u]) {
                if (u < edge.to) {                        // each undirected edge once
                    if (!components.union(u, edge.to)) {
                        return true
                    }
                } else if (u == edge.to) {                // self-loop
                    return true
                }
            }
        }

        return false
    }


    fun getTopologicalSort(): List<Int> {
        require(directed) { "Only valid for directed graphs" }
        val nodesOrder = mutableListOf<Int>()

        val degreeArray = IntArray(vertexCount) { 0 }

        for (v in 0..<vertexCount) {
            val edges = adj[v]
            edges.forEach { it ->
                degreeArray[it.to]++
            }
        }

        var queue = ArrayDeque<Int>()
        for(i in degreeArray.indices) {
            var c = degreeArray[i]
            if(c == 0) {
                queue.addLast(i)
            }
        }


        while (queue.isNotEmpty()) {
            var elem = queue.removeFirst()
            nodesOrder.add(elem)
            var edges = adj[elem]
            edges.forEach {
                degreeArray[it.to]--
                if(degreeArray[it.to] <= 0) {
                    queue.addLast(it.to)
                }
            }
        }

        check(nodesOrder.size == vertexCount) {
            "Topological sort not possible"
        }

        return nodesOrder

    }


    private data class DKNode(val vertex: Int, val distance: Int)

    fun dijkstra(source: Int): IntArray {
        require(source in 0 until vertexCount) { "vertex $source out of range" }

        val pq = PriorityQueue<DKNode>(compareBy { it.distance })
        val visited = BooleanArray(vertexCount)
        val distanceArray = IntArray(vertexCount) { Int.MAX_VALUE }

        distanceArray[source] = 0
        pq.add(DKNode(source, 0))

        while (pq.isNotEmpty()) {
            val elem = pq.poll()
            if (visited[elem.vertex]) {
                continue
            }
            visited[elem.vertex] = true

            val edges = adj[elem.vertex]

            edges.forEach {
                if (!visited[it.to]) {
                    val newDistance = distanceArray[elem.vertex] + it.weight
                    if (newDistance < distanceArray[it.to]) {
                        distanceArray[it.to] = newDistance
                        pq.add(DKNode(it.to, newDistance))
                    }
                }
            }
        }

        return distanceArray
    }


    // Single-source shortest paths, negative weights allowed. O(V * E).
    // Relax every edge V-1 times; a V-th pass that still improves something means a
    // negative cycle is reachable from source -> throws.
    // Returns dist[v] for every vertex; Int.MAX_VALUE = unreachable.
    fun bellmanFord(source: Int): IntArray {
        require(source in 0 until vertexCount) { "vertex $source out of range" }

        val distanceArray = IntArray(vertexCount) { Int.MAX_VALUE }
        distanceArray[source] = 0

        // one pass = try to improve every edge in the graph once
        fun relaxAllEdges(): Boolean {
            var improved = false
            for (u in 0 until vertexCount) {
                if (distanceArray[u] == Int.MAX_VALUE) {
                    continue                                  // unreachable so far, nothing to relax from
                }
                for (edge in adj[u]) {
                    val newDistance = distanceArray[u] + edge.weight
                    if (newDistance < distanceArray[edge.to]) {
                        distanceArray[edge.to] = newDistance
                        improved = true
                    }
                }
            }
            return improved
        }

        // a shortest path has at most V-1 edges, so V-1 passes settle everything
        repeat(vertexCount - 1) {
            if (!relaxAllEdges()) {
                return distanceArray                          // nothing changed, done early
            }
        }

        check(!relaxAllEdges()) { "graph has a negative cycle reachable from $source" }

        return distanceArray
    }


    // All-pairs shortest paths, negative weights allowed. O(V^3) time, O(V^2) space.
    // dist[i][j] after round k = shortest i -> j using only vertices 0..k as intermediates.
    // Returns the full matrix; Int.MAX_VALUE = unreachable. Throws on a negative cycle.
    fun floydWarshall(): Array<IntArray> {
        val dist = Array(vertexCount) { IntArray(vertexCount) { Int.MAX_VALUE } }

        for (i in 0 until vertexCount) {
            dist[i][i] = 0
        }
        for (u in 0 until vertexCount) {
            for (edge in adj[u]) {
                dist[u][edge.to] = minOf(dist[u][edge.to], edge.weight)   // keep the cheapest parallel edge
            }
        }

        for (k in 0 until vertexCount) {                 // allow k as an intermediate
            for (i in 0 until vertexCount) {
                if (dist[i][k] == Int.MAX_VALUE) {
                    continue                              // i can't reach k, nothing to improve via k
                }
                for (j in 0 until vertexCount) {
                    if (dist[k][j] == Int.MAX_VALUE) {
                        continue
                    }
                    val viaK = dist[i][k] + dist[k][j]
                    if (viaK < dist[i][j]) {
                        dist[i][j] = viaK
                    }
                }
            }
        }

        for (i in 0 until vertexCount) {
            check(dist[i][i] >= 0) { "graph has a negative cycle through $i" }
        }

        return dist
    }


    // Single-source shortest paths on a DAG, any weights. O(V + E).
    // Relaxing edges in topological order means every edge into v is done before v is read,
    // so one pass is enough -- no PQ, no repeated passes, negatives are fine.
    // Throws (via getTopologicalSort) if the graph has a cycle.
    fun dagShortestPath(source: Int): IntArray {
        require(directed) { "Only valid for directed graphs" }
        require(source in 0 until vertexCount) { "vertex $source out of range" }

        val order = getTopologicalSort()
        val distanceArray = IntArray(vertexCount) { Int.MAX_VALUE }
        distanceArray[source] = 0

        for (u in order) {
            if (distanceArray[u] == Int.MAX_VALUE) {
                continue
            }
            for (edge in adj[u]) {
                val newDistance = distanceArray[u] + edge.weight
                if (newDistance < distanceArray[edge.to]) {
                    distanceArray[edge.to] = newDistance
                }
            }
        }

        return distanceArray
    }


    fun findBridgesEdges(): List<MSTEdge> {
        require(!directed) { "Bridges are defined for undirected graphs" }

        val discoveryTimeArray = IntArray(vertexCount) { -1 }
        val lowestReachableArray = IntArray(vertexCount) { -1 }
        val bridgeEdges = mutableListOf<MSTEdge>()

        fun dfs(i: Int, parent: Int, currentTime: Int) {
            discoveryTimeArray[i] = currentTime
            lowestReachableArray[i] = currentTime
            var parentEdgeSkipped = false

            val edges = adj[i]
            edges.forEach {
                val node = it.to

                if (node == parent && !parentEdgeSkipped) {
                    parentEdgeSkipped = true
                    return@forEach
                }

                if (discoveryTimeArray[node] == -1) {
                    dfs(node, i, currentTime + 1)

                    // whatever node's subtree can climb to, i can climb to as well
                    lowestReachableArray[i] = minOf(lowestReachableArray[i], lowestReachableArray[node])

                    // node's subtree cannot get above i -> this edge is its only way in
                    if (lowestReachableArray[node] > discoveryTimeArray[i]) {
                        bridgeEdges.add(MSTEdge(i, node, it.weight))
                    }
                } else {
                    // back edge to an ancestor already on the DFS path
                    lowestReachableArray[i] = minOf(lowestReachableArray[i], discoveryTimeArray[node])
                }
            }
        }

        for (v in 0 until vertexCount) {
            if (discoveryTimeArray[v] == -1) {
                dfs(v, -1, 0)
            }
        }

        return bridgeEdges
    }
}


fun main() {

    val graph = AdjacencyListGraph(5)

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

    val mst = graph.primMST()
    println("primMST = $mst, total weight = ${mst.sumOf { it.weight }}")

    val mstEager = graph.primMSTEager()
    println("primMSTEager = $mstEager, total weight = ${mstEager.sumOf { it.weight }}")

    val mstKruskal = graph.kruskalMST()
    println("kruskalMST = $mstKruskal, total weight = ${mstKruskal.sumOf { it.weight }}")

    println("findBridgesEdges = ${graph.findBridgesEdges()}")

    println("dijkstra(0) = ${graph.dijkstra(0).toList()}")
    println("bellmanFord(0) = ${graph.bellmanFord(0).toList()}")

    val allPairs = graph.floydWarshall()
    println("floydWarshall:")
    allPairs.forEachIndexed { i, row -> println("  $i: ${row.toList()}") }

    graph.removeEdge(2, 3)
    println("after removeEdge(2, 3): neighbors(2) = ${graph.neighbors(2)}, edges = ${graph.edgeCount()}")
    println("findCycleUndirected = ${graph.findCycleUndirected()}")
    println("findBridgesEdges = ${graph.findBridgesEdges()}")

    val tree = AdjacencyListGraph(4)
    tree.addEdge(0, 1)
    tree.addEdge(0, 2)
    tree.addEdge(2, 3)
    println("tree: hasCycleUndirected = ${tree.hasCycleUndirected()}")
    println("hasCycleUnionFind = ${graph.hasCycleUnionFind()}, tree: ${tree.hasCycleUnionFind()}")

    println("--- directed, unweighted (default weight 1)")
    val dag = AdjacencyListGraph(4, directed = true)
    dag.addEdge(0, 1)
    dag.addEdge(0, 2)
    dag.addEdge(1, 3)
    dag.addEdge(2, 3)
    print(dag)
    println("hasEdge(0, 1) = ${dag.hasEdge(0, 1)}, hasEdge(1, 0) = ${dag.hasEdge(1, 0)}")
    println("hasCycleDirected = ${dag.hasCycleDirected()}")
    println("getTopologicalSort = ${dag.getTopologicalSort()}")

    // course prerequisites: 0 = maths, 1 = physics, 2 = programming, 3 = ML, 4 = robotics, 5 = ethics (no deps)
    val courses = AdjacencyListGraph(6, directed = true)
    courses.addEdge(0, 1)   // maths before physics
    courses.addEdge(0, 2)   // maths before programming
    courses.addEdge(2, 3)   // programming before ML
    courses.addEdge(1, 4)   // physics before robotics
    courses.addEdge(3, 4)   // ML before robotics
    println("courses order = ${courses.getTopologicalSort()}")

    dag.addEdge(3, 0)   // 0 -> 1 -> 3 -> 0
    println("after addEdge(3, 0): hasCycleDirected = ${dag.hasCycleDirected()}, cycle = ${dag.findCycleDirected()}")
    try {
        dag.getTopologicalSort()
    } catch (e: IllegalStateException) {
        println("getTopologicalSort on cyclic graph: ${e.message}")
    }

    println("--- directed, negative weight")
    val neg = AdjacencyListGraph(4, directed = true)
    neg.addEdge(0, 1, 1)
    neg.addEdge(0, 2, 2)
    neg.addEdge(2, 1, -2)    // 0 -> 2 -> 1 = 0, but Dijkstra finalizes 1 at distance 1 before seeing this
    neg.addEdge(1, 3, 1)
    println("dijkstra(0)    = ${neg.dijkstra(0).toList()}   <- wrong for 1 and 3")
    println("bellmanFord(0) = ${neg.bellmanFord(0).toList()}")
    println("floydWarshall[0] = ${neg.floydWarshall()[0].toList()}")
    println("dagShortestPath(0) = ${neg.dagShortestPath(0).toList()}")

    neg.addEdge(1, 2, -1)    // 1 -> 2 -> 1 sums to -3: negative cycle
    try {
        neg.bellmanFord(0)
    } catch (e: IllegalStateException) {
        println("bellmanFord with negative cycle: ${e.message}")
    }

}
