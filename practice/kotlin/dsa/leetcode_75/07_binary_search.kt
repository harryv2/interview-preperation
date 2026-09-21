package dsa.leetcode_75

import kotlin.math.min

class SolutionBinary {


    fun canShip(weights: IntArray, days: Int, capacity: Int): Boolean {
        var curDay = 0;
        var emptyCapacity = capacity;

        var counter = 0;

        while (curDay < days) {
            while (counter <= weights.lastIndex && emptyCapacity >= weights[counter]) {
                emptyCapacity -= weights[counter]
                counter++
            }
            curDay++
            emptyCapacity = capacity
        }

        return counter > weights.lastIndex
    }

    fun shipWithinDays(weights: IntArray, days: Int): Int {
        var minWeight = weights.max()
        var maxWeight = weights.sum()


        var start = minWeight
        var end = maxWeight

        var minCapacity = maxWeight

        while (start <= end) {
            var mid = start + (end - start) / 2

            if (canShip(weights, days, mid)) {
                minCapacity = mid
                end = mid - 1
            } else {
                start = mid + 1
            }
        }

        return minCapacity
    }


    fun canPlaceBalls(position: IntArray, m: Int, width: Int): Boolean {
        var prevPlaced = position[0]
        var counter = 1

        for (i in 1..<position.size) {
            if (position[i] >= prevPlaced + width) {
                counter++
                prevPlaced = position[i]

                if(counter == m) {
                    return true
                }
            }
        }

        return counter == m
    }

    fun maxDistance(position: IntArray, m: Int): Int {
        if (position.isEmpty()) {
            return 0
        }

        position.sort()

        var maxBound = position.last() - position.first()

        var start = 1
        var end = maxBound
        var ans = 1

        while (start <= end) {
            var mid = start + (end - start)/2

            if(canPlaceBalls(position, m, mid)) {
                ans = mid
                start = mid + 1
            } else {
                end  = mid - 1
            }
        }

        return ans
    }
}


fun main() {

    var solution = SolutionBinary()

//    println(solution.shipWithinDays(weights = intArrayOf(1,2,3,4,5,6,7,8,9,10), days = 5))


    println(
        solution.maxDistance(
            intArrayOf(5, 4, 3, 2, 1, 1000000000),
            3
        )
    )
}