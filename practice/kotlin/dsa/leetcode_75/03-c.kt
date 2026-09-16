package dsa.leetcode_75

fun maxProfitOld(prices: IntArray): Int {
    if (prices.isEmpty()) {
        return 0
    }

    var minPrice = prices[0]
    var profit = 0;

    for (i in 1..prices.lastIndex) {
        var currentPrice = prices[i]
        if (currentPrice < minPrice) {
            minPrice = currentPrice
        } else {
            profit += (currentPrice - minPrice)
            minPrice = currentPrice
        }
    }

    return profit
}


fun maxProfit(prices: IntArray): Int {

    fun maxProfitRec(i: Int, holding: Boolean, tc: Int, memo: IntArray): Int {
        if (i >= prices.size || tc > 1) {
            return 0
        }

        var key = i * 4 + (if (holding) 2 else 0) + tc

        if (memo[key] != -1) {
            return memo[key]
        }


        var answer = if (holding) {
            var skipSell = maxProfitRec(i + 1, true, tc, memo)
            var sellIt = +prices[i] + maxProfitRec(i + 1, false, tc + 1, memo)

            maxOf(skipSell, sellIt)
        } else {
            var skipBuy = maxProfitRec(i + 1, false, tc, memo)
            var buyIt = -prices[i] + maxProfitRec(i + 1, true, tc, memo)

            maxOf(skipBuy, buyIt)
        }

        memo[key] = answer

        return answer
    }

    var memo = IntArray(prices.size * 4) { -1 }

    return maxProfitRec(0, false, 0, memo)

}

fun main() {
    val a = intArrayOf(3, 3, 5, 0, 0, 3, 1, 4)
    println(maxProfit(a))
}
