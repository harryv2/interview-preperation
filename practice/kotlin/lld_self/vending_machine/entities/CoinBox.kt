package lld_self.vending_machine.entities

import lld_self.vending_machine.strategies.ChangeMakerStrategy


enum class Coin(val money: Money) {
    ONE(Money.rupees(1)),
    TWO(Money.rupees(2)),
    FIVE(Money.rupees(5)),
    TEN(Money.rupees(10)),
    TWENTY(Money.rupees(20))
}

class CoinBox(
    coins: Map<Coin, Int>,
    private val changeMakerStrategy: ChangeMakerStrategy,
) {

    val bank = HashMap<Coin, Int>()

    init {
        coins.forEach {
            bank.putIfAbsent(it.key, 0)
            bank[it.key] = bank[it.key]!! + it.value
        }
    }

    var heldCoins = mutableListOf<Coin>()


    val heldAmount: Money
        get() {
            var sum = Money.ZERO
            heldCoins.forEach {
                sum += it.money
            }
            return sum
        }


    fun addToHelp(coin: Coin) {
        heldCoins.add(coin)
    }

    private fun allCoins(): List<Coin> {
        var coins = mutableListOf<Coin>()

        bank.forEach { (coin, i) ->
            coins += MutableList(i) { coin }
        }

        return coins
    }

    fun canMakeChange(amount: Money): Boolean {
        val available = allCoins() + heldCoins
        val change = changeMakerStrategy.changeFor(amount, available)
        return change != null
    }


    fun settleUp(amount: Money): Map<Coin, Int> {
        val available = allCoins() + heldCoins

        val toReturn = heldAmount - amount
        val change = changeMakerStrategy.changeFor(toReturn, available)


        require(change != null) { "Change not made" }

        change.forEach { (coin, i) ->

            require((bank[coin] ?: 0) >= i) { "Change can not be give" }

            bank[coin] = bank[coin]!! - i
        }

        heldCoins.forEach {
            bank.putIfAbsent(it, 0)
            bank[it] = bank[it]!! + 1
        }

        heldCoins = mutableListOf<Coin>()

        return change

    }

    fun refund(): Map<Coin, Int> {
        val balance = hashMapOf<Coin, Int>()
        heldCoins.forEach {
            balance.putIfAbsent(it, 0)
            balance[it] = balance[it]!! + 1
        }
        heldCoins = mutableListOf<Coin>()
        return balance
    }

}