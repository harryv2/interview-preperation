package lld_self.vending_machine.entities


class Sale {

    private val coins = mutableListOf<Coin>()

    var selectedSlotId: SlotId? = null
        private set

    val insertedAmount: Money
        get() {
            var sum = Money.ZERO
            coins.forEach {
                sum += it.money
            }
            return sum
        }

    fun addCoin(coin: Coin) {
        coins.add(coin)
    }

    fun select(slotId: SlotId) {
        selectedSlotId = slotId
    }

    fun insertedCoins(): List<Coin> {
        return coins.toList()
    }

    fun refund(): Map<Coin, Int> {
        return coins.groupingBy { it }.eachCount()
    }
}
