package lld_self.vending_machine.entities

import lld_self.vending_machine.exceptions.ChangeUnavailableException
import lld_self.vending_machine.strategies.ChangeMakerStrategy


class CoinBank(
    initialCoins: Map<Coin, Int>,
    private val changeMakerStrategy: ChangeMakerStrategy
) {

    private val coins = HashMap<Coin, Int>()

    init {
        initialCoins.forEach { (coin, count) ->
            coins[coin] = (coins[coin] ?: 0) + count
        }
    }

    fun canSettle(incoming: List<Coin>, changeDue: Money): Boolean {
        return changeMakerStrategy.changeFor(changeDue, available() + incoming) != null
    }

    fun settle(incoming: List<Coin>, changeDue: Money): Map<Coin, Int> {
        val change = changeMakerStrategy.changeFor(changeDue, available() + incoming)
        if (change == null) {
            throw ChangeUnavailableException(changeDue)
        }

        incoming.forEach {
            coins[it] = (coins[it] ?: 0) + 1
        }

        change.forEach { (coin, count) ->
            check((coins[coin] ?: 0) >= count) { "Bank is short of $coin" }
            coins[coin] = coins[coin]!! - count
        }

        return change
    }

    private fun available(): List<Coin> {
        val all = mutableListOf<Coin>()
        coins.forEach { (coin, count) ->
            all += List(count) { coin }
        }
        return all
    }
}
