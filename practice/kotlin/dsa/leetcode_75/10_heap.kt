package dsa.leetcode_75

import java.util.PriorityQueue

class SolutionHeap {


    data class HeapNode(val sum: Int, val i: Int, val j: Int)

    fun kthSmallest(mat: Array<IntArray>, k: Int): Int {
        if (mat.isEmpty()) {
            return 0
        }

        if (mat.size == 1) {
            return if (mat[0].lastIndex >= k) mat[0][k] else 0
        }


        var currentSums = mat[0].toList()

        for (row in 1..mat.lastIndex) {
            currentSums = kSmallestPairs(currentSums, mat[row], k)
        }

        return currentSums.last()
    }


    private fun kSmallestPairs(row1: List<Int>, row2: IntArray, k: Int): List<Int> {
        val result = mutableListOf<Int>()

        var heap = PriorityQueue<HeapNode>(compareBy { it.sum })
        heap.add(HeapNode(sum = row1[0] + row2[0], 0, 0))

        val visited = Array(row1.size) { BooleanArray(row2.size) }
        visited[0][0] = true

        while (heap.isNotEmpty() && result.size < k) {
            val elem = heap.poll()

            val i = elem.i
            val j = elem.j

            result.add(elem.sum)

            if (i + 1 in 0..row1.lastIndex && !visited[i + 1][j]) {
                visited[i + 1][j] = true
                heap.add(HeapNode(row1[i + 1] + row2[j], i + 1, j))
            }


            if (j + 1 in 0..row2.lastIndex && !visited[i][j + 1]) {
                visited[i][j + 1] = true
                heap.add(HeapNode(row1[i] + row2[j + 1], i, j + 1))
            }
        }

        return result
    }


    data class RoomMeeting(val endTime: Int, val roomNum: Int)


    fun mostBooked(n: Int, meetings: Array<IntArray>): Int {
        meetings.sortBy { it[0] }

        var counts = IntArray(n)

        var availableRooms = PriorityQueue<Int>()
        for (i in 0..<n) {
            availableRooms.add(i)
        }

        val busyRooms = PriorityQueue<RoomMeeting> { a, b ->
            if (a.endTime == b.endTime) {
                a.roomNum.compareTo(b.roomNum)
            } else {
                a.endTime.compareTo(b.endTime)
            }
        }


        for(meeting in meetings) {
            var startTime = meeting[0]
            var endTime = meeting[1]
            var duration = endTime - startTime

            // Free room
            while (busyRooms.isNotEmpty() && busyRooms.peek().endTime <= startTime) {
                availableRooms.add(busyRooms.poll().roomNum)
            }


            if(availableRooms.isNotEmpty()) {
                var room = availableRooms.poll()
                busyRooms.add(RoomMeeting(endTime, room))
                counts[room]++
            } else{
                val earliestMeeting = busyRooms.poll()
                busyRooms.add(RoomMeeting(duration + earliestMeeting.endTime, earliestMeeting.roomNum))
                counts[earliestMeeting.roomNum]++
            }
        }

        return counts.indices.maxByOrNull { counts[it] } ?: -1
    }
}


fun main() {

    val solutionHeap = SolutionHeap()


//    println(
//        solutionHeap.kthSmallest(
//            arrayOf(
//                intArrayOf(1, 3, 11),
//                intArrayOf(2, 4, 6)
//            ),
//            5
//        )
//    )


    val meetings: Array<IntArray> = arrayOf(
        intArrayOf(0, 10),
        intArrayOf(1, 5),
        intArrayOf(2, 7),
        intArrayOf(3, 4)
    )

    println(
        solutionHeap.mostBooked(
            n = 2, meetings = meetings
        )
    )
}