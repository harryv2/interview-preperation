package dsa.leetcode_75

class SolutionGameTheory {

    fun stoneGame(piles: IntArray): Boolean {
        val memo = Array(piles.size) { IntArray(piles.size) { -1 } }

        fun dp(i: Int, j: Int): Int {
            if (i == j) {
                return piles[i]
            }

            if (memo[i][j] != -1) {
                return memo[i][j]
            }

            val pickLeft = piles[i] - dp(i + 1, j)
            val pickRight = piles[j] - dp(i, j + 1)

            val ans = maxOf(pickLeft, pickRight)

            memo[i][j] = ans
            return ans
        }

        return dp(0, piles.lastIndex) > 0
    }

    fun stoneGameVII(stones: IntArray): Int {

        val prefixSum = IntArray(stones.size)

        var sum = 0
        for ((i, s) in stones.withIndex()) {
            sum += s
            prefixSum[i] = sum
        }


        fun sum(i: Int, j: Int): Int {
            return prefixSum[j] - prefixSum.getOrElse(i - 1) { 0 }
        }

        val memo = Array(stones.size) { IntArray(stones.size) { -1 } }

        fun dp(i: Int, j: Int): Int {
            if (i == j) {
                return 0
            }

            if (memo[i][j] != -1) {
                return memo[i][j]
            }

            val pickLeft = sum(i + 1, j) - dp(i + 1, j)
            val pickRight = sum(i, j - 1) - dp(i, j - 1)

            val ans = maxOf(pickLeft, pickRight)

            memo[i][j] = ans

            return ans
        }

        return dp(0, stones.lastIndex)
    }
}


fun main() {
    val solutionBKT = SolutionGameTheory()

    println(solutionBKT.stoneGameVII(intArrayOf(5, 3, 1, 4, 2)))
}