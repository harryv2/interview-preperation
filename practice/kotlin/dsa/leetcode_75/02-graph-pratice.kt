package dsa.leetcode_75

class Solution {

    fun solve(board: Array<CharArray>): Unit {
        if (board.isEmpty() || board[0].isEmpty()) {
            return
        }
        var m = board.size
        var n = board[0].size


        var directions = arrayOf<Pair<Int, Int>>(
            Pair(0, 1), // right
            Pair(1, 0), // bottom
            Pair(-1, 0), // up
            Pair(0, -1) // down
        )

        val parentConnectedToEdge = hashMapOf<Pair<Int, Int>, Boolean>()
        val parentArray = Array(m) { Array<Pair<Int, Int>?>(n) { null } }

        fun dfs(i: Int, j: Int, parent: Pair<Int, Int>) {
            if (parentArray[i][j] != null) {
                return
            }

            parentArray[i][j] = parent
            var isEdge = i == m - 1 || j == n - 1
            if (isEdge) {
                parentConnectedToEdge[parent] = true
            }

            for (dir in directions) {
                var nextI = i + dir.first
                var nextJ = j + dir.second

                if (nextI in 0..<m && nextJ in 0..n && board[nextI][nextJ] == 'O') {
                    if (parentArray[nextI][nextJ] != null) {
                        continue
                    }
                    dfs(nextI, nextJ, parent)
                }
            }

        }


        for (i in 0..<m) {
            for (j in 0..<n) {
                if (board[i][j] == 'O') {
                    if (parentArray[i][j] == null) {
                        dfs(i, j, Pair(i, j))
                    }

                    var parent = parentArray[i][j]
                    if (!parentConnectedToEdge.getOrDefault(parent, false)) {
                        board[i][j] = 'X'
                    }
                }
            }
        }


    }


    class Edge(val to: Int)

    fun findOrder(numCourses: Int, prerequisites: Array<IntArray>): IntArray {
        var graph = Array<MutableList<Edge>>(numCourses) { mutableListOf() }
        var inDegree = IntArray(numCourses) { 0 }

        for (preq in prerequisites) {
            var source = preq[0]
            var destination = preq[1]
            graph[source].add(Edge(destination))
            inDegree[destination]++
        }


        var queue = ArrayDeque<Int>()

        var startCourses = inDegree.indices.filter { inDegree[it] == 0 }

        startCourses.forEach { queue.addLast(it) }

        var visited = BooleanArray(numCourses)
        var sortOrder = mutableListOf<Int>()

        while (queue.isNotEmpty()) {
            var elem = queue.removeFirst()
            visited[elem] = true
            sortOrder.add(elem)
            var edges = graph[elem]
            edges.forEach {
                inDegree[it.to]--
                if (!visited[it.to] && inDegree[it.to] <= 0) {
                    queue.add(it.to)
                }
            }
        }

        if (sortOrder.size == numCourses) {
            return IntArray(0)
        }

        return sortOrder.toIntArray()

    }


}


fun main() {

    val board: Array<CharArray> = arrayOf(
        charArrayOf('X', 'X', 'X', 'X'),
        charArrayOf('X', 'O', 'O', 'X'),
        charArrayOf('X', 'X', 'O', 'X'),
        charArrayOf('X', 'O', 'O', 'X')
    )

    val solu = Solution()

    solu.solve(board)

    print(board.joinToString { it -> it.joinToString(", ") + "\n" })
}