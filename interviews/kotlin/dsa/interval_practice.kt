package dsa

data class Degree(var inDeg: Int = 0, var outDeg: Int = 0)

fun toplogy(num: Int, prerequisites: Array<IntArray>): List<Int> {
    var adjList = HashMap<Int, MutableList<Int>>()

    for(edge in prerequisites) {
        var source = edge[1]
        var destiation = edge[0]

        var list = adjList.getOrDefault(source, mutableListOf<Int>())
        list.add(destiation)
        adjList[source] = list
    }

    var degreeMap = HashMap<Int, Degree>()

    for((k, v) in adjList.entries) {
        var elem = degreeMap.getOrDefault(k, Degree())
        elem.outDeg = v.size
        degreeMap[k] = elem

        for(child in v) {
            var elemC = degreeMap.getOrDefault(child, Degree())
            elemC.inDeg++
            degreeMap[child]  = elemC
        }
    }


    var bfsSource = degreeMap.entries.find { v -> v.value.inDeg == 0 }

    var visited = HashMap<Int, Boolean>()

    var bfsQueue = ArrayDeque<Int>()

    bfsQueue.addLast(bfsSource?.key!!)
    visited[bfsSource.key] = true;

    var answer = mutableListOf<Int>(bfsSource.key)

    while (bfsQueue.isNotEmpty()) {
        var top = bfsQueue.removeFirst()

        var children = adjList[top] ?: emptyList()

        for(child in children) {
            if(visited[child] == true) {
               continue
            }

            bfsQueue.addLast(child)
            answer.add(child)
            visited[child] = true
        }
    }

    return answer

}


fun main() {

    // [[1,0],[2,0],[3,1],[3,2]]
    var numCourses = 4;
    var prerequisites = arrayOf(
        intArrayOf(1, 0),
        intArrayOf(2, 0),
        intArrayOf(3, 1),
        intArrayOf(3, 2)
    )

    println(toplogy(numCourses,prerequisites))
}