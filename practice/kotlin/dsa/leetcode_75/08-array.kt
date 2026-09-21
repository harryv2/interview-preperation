package dsa.leetcode_75

import kotlin.math.min

class SolutionArray {
    fun subarraySumOld(nums: IntArray, k: Int): Int {
        var prefixSum = IntArray(nums.size) { 0 }

        var sum = 0;
        for ((i, num) in nums.withIndex()) {
            sum += num
            prefixSum[i] = sum
        }

        var count = 0

        for (i in 0..<nums.size) {
            for (j in i..<nums.size) {
                var sum = prefixSum[j] - prefixSum.getOrElse(i - 1) { 0 }
                if (sum == k) {
                    count++
                }
            }
        }

        return count
    }

    fun subarraySum(nums: IntArray, k: Int): Int {
        var count = 0
        var currentSum = 0

        val prefixSumCounts = HashMap<Int, Int>()

        prefixSumCounts[0] = 1

        for (num in nums) {
            currentSum += num

            val target = currentSum - k
            if (prefixSumCounts.contains(target)) {
                count += prefixSumCounts[target]!!
            }

            prefixSumCounts[currentSum] = prefixSumCounts.getOrDefault(currentSum, 0) + 1
        }

        return count
    }
}


fun main() {

    var sol = SolutionArray()

    println(sol.subarraySum(intArrayOf(1, 2, 3), k = 3))

}