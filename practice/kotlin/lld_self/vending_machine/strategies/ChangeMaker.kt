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
        return emptyMap()
    }

}