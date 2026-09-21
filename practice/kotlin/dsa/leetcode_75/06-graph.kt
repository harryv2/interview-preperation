package dsa.leetcode_75

import dsa.mycodeschool.unionfind.UnionFind

class SolutionGrph6 {

    data class Edge(val to: Int, val weight: Int)

    fun findCheapestPrice(n: Int, flights: Array<IntArray>, src: Int, dst: Int, k: Int): Int {
        var adjList = Array<MutableList<Edge>>(n) { mutableListOf() }

        for (f in flights) {
            var source = f[0]
            var destination = f[1]
            var weight = f[2]

            adjList[source].add(Edge(destination, weight))
        }


        var distances = IntArray(n) { Int.MAX_VALUE }
        distances[src] = 0


        fun bellmanRelaxEdges() {
            val prevDistances = distances.copyOf()

            for (vertex in 0..<n) {
                if (prevDistances[vertex] == Int.MAX_VALUE) {
                    continue
                }

                var edges = adjList[vertex]
                edges.forEach {
                    val to = it.to
                    val weight = it.weight

                    distances[to] = minOf(distances[to], prevDistances[vertex] + weight)
                }
            }
        }


        repeat(k + 1) {
            bellmanRelaxEdges()
        }

        if (distances[dst] == Int.MAX_VALUE) {
            return -1
        }

        return distances[dst]
    }


    data class Node(val i: Int, val j: Int, val distance: Int)

    fun maximumSafenessFactor(grid: List<List<Int>>): Int {
        if (grid.isEmpty()) {
            return 0
        }

        var m = grid.size
        var n = grid.size

        var manhattan = Array(m) { IntArray(n) { Int.MAX_VALUE } }


        var queue = ArrayDeque<Node>()
        for (i in 0..<m) {
            for (j in 0..<n) {
                if (grid[i][j] == 1) {
                    queue.add(Node(i, j, 0))
                }
            }
        }

        var directions = listOf<Pair<Int, Int>>(
            Pair(0, 1),
            Pair(1, 0),
            Pair(-1, 0),
            Pair(0, -1)
        )

        var maxDistance = Int.MIN_VALUE

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            if (manhattan[node.i][node.j] != Int.MAX_VALUE) {
                continue
            }

            manhattan[node.i][node.j] = node.distance
            maxDistance = maxOf(maxDistance, node.distance)

            for (dir in directions) {
                var nextI = node.i + dir.first
                var nextJ = node.j + dir.second

                if (nextI in 0..<m && nextJ in 0..<n && manhattan[nextI][nextJ] == Int.MAX_VALUE) {
                    queue.addLast(Node(nextI, nextJ, node.distance + 1))
                }
            }
        }


        var start = 0
        var end = maxDistance

        var minSafe = Int.MAX_VALUE

        while (start <= end) {
            var mid = start + (end - start) / 2

            if (canReach(manhattan, mid)) {
                minSafe = mid
                start = mid + 1
            } else {
                end = mid - 1
            }
        }

        return minSafe

    }


    fun canReach(manhattanDistance: Array<IntArray>, targetFactor: Int): Boolean {
        var m = manhattanDistance.size
        var n = manhattanDistance.size

        if (manhattanDistance[0][0] < targetFactor || manhattanDistance[m - 1][n - 1] < targetFactor) {
            return false
        }

        var directions = listOf(
            Pair(0, 1),
            Pair(1, 0),
            Pair(-1, 0),
            Pair(0, -1)
        )

        val visited = Array(m) { BooleanArray(n) }

        fun dfs(i: Int, j: Int): Boolean {
            if (i == m - 1 && j == n - 1) {
                return true
            }

            if (i >= m) {
                return false
            }

            if (j >= n) {
                return false
            }

            visited[i][j] = true

            for (dir in directions) {
                var nextI = i + dir.first
                var nextJ = j + dir.second

                if (nextI in 0..<m && nextJ in 0..<n && !visited[nextI][nextJ] && manhattanDistance[nextI][nextJ] >= targetFactor) {
                    if (dfs(nextI, nextJ)) {
                        return true
                    }
                }
            }

            return false
        }

        return dfs(0, 0)
    }


    fun minMalwareSpread(graph: Array<IntArray>, initial: IntArray): Int {
        if (graph.isEmpty()) {
            return 0
        }

        var n = graph.size

        var unionFind = UnionFind(n)

        for (i in 0..<n) {
            for (j in 0..<n) {
                if (graph[i][j] == 1) {
                    unionFind.union(i, j)
                }
            }
        }

        initial.sort()
        var maxIslandSizeWithOneInfected = Int.MIN_VALUE
        var removedInitial = initial[0]

        for (i in initial) {
            var groupSize = unionFind.groupSize(i)
            var allDisjoint = initial.all { i == it || !unionFind.isConnected(i, it) }
            if (allDisjoint && maxIslandSizeWithOneInfected < groupSize) {
                maxIslandSizeWithOneInfected = groupSize
                removedInitial = i
            }
        }

        return removedInitial

    }


    fun numIslands(grid: Array<CharArray>): Int {
        if (grid.isEmpty()) {
            return 0
        }

        val m = grid.size
        val n = grid[0].size

        var directions = listOf(
            Pair(0, 1),
            Pair(1, 0),
            Pair(-1, 0),
            Pair(0, -1)
        )

        var visited = Array(m) { BooleanArray(n) }

        fun dfs(i: Int, j: Int) {
            if (visited[i][j]) {
                return
            }

            visited[i][j] = true

            for (dir in directions) {
                var nextI = i + dir.first
                var nextJ = j + dir.second

                if (nextI in 0 until m && nextJ in 0 until n && !visited[nextI][nextJ] && grid[nextI][nextJ] == '1') {
                    dfs(nextI, nextJ)
                }
            }
        }

        var count = 0

        for (i in 0 until m) {
            for (j in 0 until n) {
                if (grid[i][j] == '1' && !visited[i][j]) {
                    count++
                    dfs(i, j)
                }
            }
        }

        return count
    }
}


fun main() {


//    val data: Array<IntArray> = arrayOf(
//        intArrayOf(0, 1, 100),
//        intArrayOf(1, 2, 100),
//        intArrayOf(2, 0, 100),
//        intArrayOf(1, 3, 600),
//        intArrayOf(2, 3, 200)
//    )

    val solution = SolutionGrph6()

//    println(solution.findCheapestPrice(4, data, 0, 3, 1))
//
//
//    val data = listOf(
//        listOf(0, 1, 1),
//        listOf(0, 1, 1),
//        listOf(1, 1, 0),
//    )
//
//    print(solution.maximumSafenessFactor(data))


    val grid: Array<CharArray> = arrayOf(
        charArrayOf('1', '1', '0', '0', '0'),
        charArrayOf('1', '1', '0', '0', '0'),
        charArrayOf('0', '0', '1', '0', '0'),
        charArrayOf('0', '0', '0', '1', '1')
    )

    println(solution.numIslands(grid))
}