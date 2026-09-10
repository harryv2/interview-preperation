package dsa.leetcode_75

class SolutionDP {

    fun tribonacci(n: Int): Int {
        var ansers = IntArray(maxOf(n + 1, 3))

        ansers[0] = 0
        ansers[1] = 1
        ansers[2] = 1

        if (n <= 2) {
            return ansers[n]
        }

        for (i in 3..n) {
            ansers[i] = ansers[i - 1] + ansers[i - 2] + ansers[i - 3]
        }

        return ansers[n]
    }

    private fun minCostClimbingStairsRec(cost: IntArray, currentIdc: Int, memo: IntArray): Int {
        if (currentIdc in 0..memo.lastIndex && memo[currentIdc] != -1) {
            return memo[currentIdc]
        }

        if (currentIdc > cost.lastIndex) {
            return 0
        }

        var ans = cost.getOrElse(currentIdc) { 0 } + minOf(
            minCostClimbingStairsRec(cost, currentIdc + 1, memo),
            minCostClimbingStairsRec(cost, currentIdc + 2, memo)
        )

        if (currentIdc in 0..memo.lastIndex) {
            memo[currentIdc] = ans
        }

        return ans

    }

    fun minCostClimbingStairs(cost: IntArray): Int {
        var memo = IntArray(cost.size)
        memo.fill(-1)
        return minCostClimbingStairsRec(cost, -1, memo)
    }


    private fun robsRec(nums: IntArray, currentIdc: Int, memo: IntArray): Int {
        if (currentIdc in 0..memo.lastIndex && memo[currentIdc] != -1) {
            return memo[currentIdc]
        }

        if (currentIdc > nums.lastIndex) {
            return 0
        }

        var ans = maxOf(
            (nums.getOrNull(currentIdc) ?: 0) + robsRec(nums, currentIdc + 2, memo),
            robsRec(nums, currentIdc + 1, memo)
        )

        if (currentIdc in 0..memo.lastIndex) {
            memo[currentIdc] = ans
        }

        return ans

    }

    fun rob(nums: IntArray): Int {
        var memo = IntArray(nums.size)
        memo.fill(-1)
        return robsRec(nums, -1, memo)
    }


    fun numTilingsRec(n: Int, memo: IntArray): Int {
        if (memo.getOrNull(n) != -1) {
            return memo[n]
        }

        if (n < 0) {
            return 0
        }

        when (n) {
            1 -> {
                return 1
            }

            2 -> {
                return 2
            }

            3 -> {
                return 5
            }
        }

        var ans = (1 * numTilingsRec(n - 1, memo) + 2 * numTilingsRec(n - 2, memo) + 3 * numTilingsRec(
            n - 3,
            memo
        )) % 10_000_000

        memo[n] = ans

        return ans
    }

    fun numTilings(n: Int): Int {
        var memo = IntArray(n + 1) { -1 }
        return numTilingsRec(n, memo)
    }


    fun uniquePathsDP(m: Int, n: Int, maxM: Int, maxN: Int, memo: Array<IntArray>): Int {

        if (memo[m][n] != -1) {
            return memo[m][n]
        }

        if (m == maxM - 1 && n == maxN - 1) {
            return 1
        }

        if (m >= maxM || n >= maxN) {
            return 0
        }

        val ans = uniquePathsDP(m + 1, n, maxM, maxN, memo) + uniquePathsDP(m, n + 1, maxM, maxN, memo)

        memo[m][n] = ans

        return ans

    }

    fun uniquePaths(m: Int, n: Int): Int {
        var memo = Array(m) { IntArray(n) { -1 } }
        return uniquePathsDP(0, 0, m, n, memo)
    }
}

fun main() {
    var solution = SolutionDP()

//    print(solution.tribonacci(25))

    print(solution.uniquePaths(3, 7))

}