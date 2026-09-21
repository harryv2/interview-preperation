package dsa.leetcode_75
class SolutionHashMap() {
    fun firstMissingPositive(nums: IntArray): Int {
        var map = nums.associateWith { it -> true }

        var maxElem = nums.max()

        if(maxElem <= 0) {
            return 1
        }

        if(maxElem == Int.MAX_VALUE) {
            for(i in 1..<maxElem) {
                if (!map.contains(i)) {
                    return i
                }
            }
        }

        for(i in 1..maxElem + 1) {
            if (!map.contains(i)) {
                return i
            }
        }

        return -1
    }
}


fun main() {
    var sol = SolutionHashMap()

    println(sol.firstMissingPositive(intArrayOf(1,2,3)))

}
