package dsa.leetcode_75

import kotlin.math.abs

class SolutionDP7 {

    fun lengthOfLIS(nums: IntArray, k: Int): Int {
        if (nums.isEmpty()) return 0

        val n = nums.size
        val memo = Array(n) { IntArray(n + 1) { -1 } }

        fun lisDP(i: Int, prevI: Int): Int {
            if (i >= n) return 0

            val memoIndex = prevI + 1
            if (memo[i][memoIndex] != -1) {
                return memo[i][memoIndex]
            }

            var ans = lisDP(i + 1, prevI)

            if (prevI == -1 || (nums[i] > nums[prevI] && nums[i] - nums[prevI] <= k)) {
                ans = maxOf(ans, 1 + lisDP(i + 1, i))
            }

            memo[i][memoIndex] = ans
            return ans
        }

        return lisDP(0, -1)
    }

    fun lengthOfLISO(nums: IntArray, k: Int): Int {
        if (nums.isEmpty()) {
            return 0
        }

        var n = nums.size

        var memo = Array(n) { IntArray(n) { -1 } }


        fun lisDP(i: Int, prevI: Int): Int {
            if (i >= n) {
                return 0
            }

            if (prevI in 0..<n && memo[i][prevI] != -1) {
                return memo[i][prevI]
            }

            var ans = if (prevI == -1 || (nums[i] - nums[prevI] >= k)) {
                maxOf(
                    1 + lisDP(i + 1, i),
                    lisDP(i + 1, prevI)
                )
            } else {
                lisDP(i + 1, prevI)
            }

            if (prevI in 0..n) {
                memo[i][prevI] = ans
            }

            return ans
        }

        return lisDP(0, -1)
    }

    data class Count(var zero: Int, var one: Int)

    fun findMaxForm(strs: Array<String>, m: Int, n: Int): Int {
        if (strs.isEmpty()) {
            return 0
        }

        val countArray = Array(strs.size) { Count(0, 0) }

        for ((i, str) in strs.withIndex()) {
            for (chr in str) {
                if (chr == '1') {
                    countArray[i].one++
                } else {
                    countArray[i].zero++
                }
            }
        }

        val memo = HashMap<String, Int>()

        fun dp(i: Int, capOne: Int, capZero: Int): Int {
            if (i >= strs.size) {
                return 0
            }

            var key = "$i--$capOne--$capZero"
            if (memo.contains(key)) {
                return memo[key]!!
            }


            val skipCurrent = dp(i + 1, capOne, capZero)

            val ans = if (capOne >= countArray[i].one && capZero >= countArray[i].zero) {
                val takeCurrent = 1 + dp(i + 1, capOne - countArray[i].one, capZero - countArray[i].zero)
                maxOf(skipCurrent, takeCurrent)
            } else {
                skipCurrent
            }

            memo[key] = ans

            return ans
        }

        return dp(0, n, m)
    }


    val MOD = 1_000_000_007


    fun countSteppingNumbers(low: String, high: String): Int {
        var addi = if (isSteppingNumber(low)) 1 else 0

        return ((countRangesTill(high) - countRangesTill(low) + addi) % MOD + MOD) % MOD
    }

    fun countRangesTill(upperBoundStr: String): Int {

        var memo = HashMap<String, Int>()

        fun dp(index: Int, prevDigit: Int, isBoundByLimit: Boolean, isLeadingZero: Boolean): Int {
            if (index >= upperBoundStr.length) {
                return if (isLeadingZero) 0 else 1
            }

            var key = "$index-$prevDigit-$isBoundByLimit-$isLeadingZero"

            if (memo.contains(key)) {
                return memo[key]!!
            }

            var totalWays = 0
            var limit = if (isBoundByLimit) upperBoundStr[index].digitToInt() else 9

            for (i in 0..limit) {

                val isNextBoundByLimit = isBoundByLimit && (i == limit)

                if (isLeadingZero) {
                    if (i == 0) {
                        val waysFromSkippingDigit = dp(index + 1, 10, isNextBoundByLimit, true)
                        totalWays = (totalWays + waysFromSkippingDigit) % MOD
                    } else {
                        val waysFromStartingNewNumber = dp(index + 1, i, isNextBoundByLimit, false)
                        totalWays = (totalWays + waysFromStartingNewNumber) % MOD

                    }
                } else {
                    if (abs(prevDigit - i) == 1) {
                        var waysFromContinuingNumber = dp(index + 1, i, isNextBoundByLimit, false)
                        totalWays = (totalWays + waysFromContinuingNumber) % MOD
                    }
                }
            }

            memo[key] = totalWays

            return totalWays

        }

        return dp(0, 10, isBoundByLimit = true, isLeadingZero = true)
    }

    private fun isSteppingNumber(numberStr: String): Boolean {
        for (i in 1 until numberStr.length) {
            if (abs(numberStr[i] - numberStr[i - 1]) != 1) {
                return false
            }
        }
        return true
    }

}

fun main() {

    var sol = SolutionDP7()


//    println(sol.lengthOfLIS(intArrayOf(4, 2, 1, 4, 3, 4, 5, 8, 15), k = 3))

//    println(sol.findMaxForm(
//        arrayOf<String>("10","0001","111001","1","0"),
//        5, 3
//    ))

    println(sol.countSteppingNumbers("90", "101"))
}