package dsa.leetcode_75

fun climbStairs(n: Int): Int {

    var memo = IntArray(n) { -1 }

    fun climbStairsDp(i: Int): Int {
        if (i == n) {
            return 1
        }

        if (i > n) {
            return 0
        }

        if (memo[i] != -1) {
            return memo[i]
        }


        val answer = climbStairsDp(i + 1) + climbStairsDp(i + 2)

        memo[i] = answer

        return answer
    }

    return climbStairsDp(0)
}


fun wordBreak(s: String, wordDict: List<String>): Boolean {

    var wordsMap = wordDict.toHashSet()

    fun wordBreakDP(i: Int, prevBreak: Int, words: MutableList<String>): Boolean {
        if (i == s.lastIndex) {
            if (prevBreak != i) {
                words.addLast(s.substring(prevBreak..i))
            }
            var ans = words.all { wordsMap.contains(it) }
            if (prevBreak != i) {
                words.removeLast()
            }
            return ans
        }

        var word = s.substring(prevBreak..i)
        words.addLast(word)
        var breakHere = wordBreakDP(i + 1, i + 1, words)
        words.removeLast()


        var skipBreak = wordBreakDP(i + 1, prevBreak, words)

        return breakHere || skipBreak
    }

    return wordBreakDP(0, 0, mutableListOf<String>())
}


fun coinChange(coins: IntArray, amount: Int): Int {

    var memo = IntArray(amount + 1) { -1 }

    fun coinChangeDP(n: Int): Int {
        if (n <= 0) {
            return 0
        }

        if (memo[n] != -1) {
            return memo[n]
        }

        var minVal = Int.MAX_VALUE

        for (coin in coins) {
            if (n - coin >= 0) {
                val ans = coinChangeDP(n - coin)
                if (ans == Int.MAX_VALUE) {
                    continue
                }

                minVal = minOf(minVal, 1 + ans)
            }
        }

        memo[n] = minVal

        return minVal
    }


    var ans = coinChangeDP(amount)

    if (ans == Int.MAX_VALUE) {
        return -1
    }

    return ans
}


fun minimumTotal(triangle: List<List<Int>>): Int {
    if (triangle.isEmpty()) {
        return 0
    }

    var m = triangle.size

    var memo = Array(m){ IntArray(it + 1){-1} }

    fun minimumTotalDP(i: Int, j: Int): Int {
        if (i >= m || j >= triangle[i].size) {
            return 0
        }

        if(memo[i][j] != -1) {
            return memo[i][j]
        }

        var takeJ =  triangle[i][j] + minimumTotalDP(i+1, j)
        var takeJPlus = triangle[i][j] + minimumTotalDP(i+1, j+1)

        var answer =  minOf(takeJ, takeJPlus)

        memo[i][j] = answer

        return answer
    }

    return minimumTotalDP(0,0)
}

fun minPathSum(grid: Array<IntArray>): Int {
    if (grid.isEmpty()) {
        return 0
    }

    var m = grid.size
    var n = grid[0].size

    var memo = Array(m) { IntArray(n) { -1 } }

    fun minPathSumDP(i: Int, j: Int): Int {
        if (i >= m || j >= n) {
            return Int.MAX_VALUE
        }

        if (i == m - 1 && j == n - 1) {
            return grid[i][j]
        }

        if (memo[i][j] != -1) {
            return memo[i][j]
        }

        var down = minPathSumDP(i + 1, j)
        var right = minPathSumDP(i, j + 1)

        var answer = grid[i][j] + minOf(down, right)

        memo[i][j] = answer

        return answer
    }

    return minPathSumDP(0, 0)
}


fun uniquePathsWithObstacles(obstacleGrid: Array<IntArray>): Int {
    if (obstacleGrid.isEmpty()) {
        return 0
    }

    var m = obstacleGrid.size
    var n = obstacleGrid[0].size

    var memo = Array(m){ IntArray(n){-1} }

    fun dp(i: Int, j: Int): Int {
        if(i >= m || j >= n) {
            return 0
        }

        if(obstacleGrid[i][j] == 1) {
            return 0
        }

        if(i == m- 1 && j == n - 1) {
            return 1
        }

        if(memo[i][j] != -1) {
            return memo[i][j]
        }


        var right = dp(i, j+1)
        var down = dp(i + 1, j)

        memo[i][j] =  right + down

        return memo[i][j]
    }

    return dp(0, 0)
}



fun longestPalindrome(s: String): String {
    if(s.isEmpty()) {
        return s
    }

    if(s.length == 1) {
        return s
    }


    var memo = Array(s.length){ IntArray(s.length){-1} }


    fun isPal(i: Int, j: Int): Boolean {
        if(i >= j) {
            return true
        }

        if(s[i] != s[j]) {
            return false
        }

        if(memo[i][j] != -1) {
            return memo[i][j] == 1
        }

        var ans=  isPal(i +1, j -1)

        memo[i][j] =   if (ans) 1 else 0

        return ans
    }

    var bestLength = 0
    var bestI = -1
    var bestJ = -1

    for (i in 0..s.lastIndex) {
        for(j in i..s.lastIndex) {
            if(isPal(i, j)) {
                var currentLength = j - i + 1
                if(currentLength >= bestLength) {
                    bestI = i
                    bestJ = j
                    bestLength = currentLength
                }
            }
        }
    }

    if(bestI == -1) {
        return ""
    }

    return s.substring(bestI..bestJ)

}



fun isInterleave(s1: String, s2: String, s3: String): Boolean {
    if(s1.length + s2.length != s3.length) {
        return false
    }

    var memo = Array<IntArray>(s1.length + 1){ IntArray(s2.length + 1){-1} }

    fun dp(i: Int, j: Int): Boolean {
        if (i == s1.length && j == s2.length) {
            return true
        }

        if(memo[i][j] != -1) {
            return memo[i][j] == 1
        }

        var takeS1 = i < s1.length && s1[i] == s3[i + j] && dp(i + 1, j)
        var takeS2 = j < s2.length && s2[j] == s3[i + j] && dp(i, j + 1)

        val ans =   takeS1 || takeS2

        memo[i][j] = if(ans) 1 else 0

        return ans
    }

    return dp(0, 0)
}

fun main() {

//    print(climbStairs(3))


//    print(wordBreak(s = "catsdog", wordDict = listOf<String>("cats","dog","sand","and","cat")))

//    print(coinChange(intArrayOf(1, 2, 5), 11))


//    print(
//        minimumTotal(
//            listOf(
//                listOf(-10),
//
//            )
//        )
//    )

    val grid = arrayOf(
        intArrayOf(0, 0, 0),
        intArrayOf(0, 1, 0),
        intArrayOf(0, 0, 0)
    )

//    print(uniquePathsWithObstacles(grid))


//    print(longestPalindrome("babad"))


    print(isInterleave( s1 = "aabcc", s2 = "dbbca", s3 = "aadbbcbcac"))
}