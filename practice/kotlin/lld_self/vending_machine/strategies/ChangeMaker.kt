package lld_self.vending_machine.strategies

import lld_self.vending_machine.entities.Coin
import lld_self.vending_machine.entities.Money

interface ChangeMakerStrategy {
    fun changeFor(amount: Money, bank: List<Coin>): Map<Coin, Int>?
}


class GreedyChangeMaker : ChangeMakerStrategy {

    override fun changeFor(
        amount: Money,
        bank: List<Coin>
    ): Map<Coin, Int>? {
        require(amount >= Money.ZERO) { "Amount can not be negative" }

        val available = bank.groupingBy { it }.eachCount()
        val picked = HashMap<Coin, Int>()
        var remaining = amount

        for (coin in Coin.entries.sortedByDescending { it.money }) {
            var count = available[coin] ?: 0

            while (count > 0 && coin.money <= remaining) {
                remaining -= coin.money
                picked[coin] = (picked[coin] ?: 0) + 1
                count -= 1
            }
        }

        if (remaining != Money.ZERO) {
            return null
        }

        return picked
    }

}
