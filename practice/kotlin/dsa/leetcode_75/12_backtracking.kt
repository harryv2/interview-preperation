package dsa.leetcode_75

class SolutionBKT {
    fun permuteUnique(nums: IntArray): List<List<Int>> {

        val results = mutableListOf<List<Int>>()

        nums.sort()

        val used = BooleanArray(nums.size)


        fun backtracking(currentPath: MutableList<Int>) {
            if(currentPath.size == nums.size) {
                results.add(currentPath.toList())
                return
            }

            for(i in nums.indices) {
                if(used[i]) {
                    continue
                }

                if (i > 0 && nums[i] == nums[i - 1] && !used[i - 1]) {
                    continue
                }

                currentPath.add(nums[i])
                used[i] = true
                backtracking(currentPath)
                used[i] = false
                currentPath.removeLast()
            }
        }

        backtracking(mutableListOf<Int>())

        return results
    }
}


fun main() {
    val solutionBKT = SolutionBKT()

    println(solutionBKT.permuteUnique(intArrayOf(1, 1, 2)))
}