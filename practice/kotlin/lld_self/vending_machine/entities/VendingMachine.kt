package lld_self.vending_machine.entities

import lld_self.vending_machine.strategies.ChangeMakerStrategy


enum class VendingMachineState {
    IDLE,
    COIN_INSERTED,
    DISPENSING,
    OUT_OF_SERVICE
}

class VendingMachine(
    val id: String,
    coins: Map<Coin, Int>,
    initialInventory: List<VendingSlot>,
    changeMakerStrategy: ChangeMakerStrategy
) {

    internal val coinBox = CoinBox(coins, changeMakerStrategy)
    internal val inventory = Inventory(initialInventory)

    private var state = if (inventory.isCompletelyEmpty()) {
        VendingMachineState.OUT_OF_SERVICE
    } else {
        VendingMachineState.IDLE
    }

    internal var selectedSlotId: SlotId? = null


    private val stateWiseHandlerMap = mapOf(
        VendingMachineState.IDLE to IdleState(this),
        VendingMachineState.COIN_INSERTED to CoinInsertedState(this),
        VendingMachineState.DISPENSING to DispensingState(this),
        VendingMachineState.OUT_OF_SERVICE to OutOfServiceState(this)
    )

    fun insertedAmount(): Money {
        return coinBox.heldAmount
    }

    fun insertCoin(coin: Coin) {
        stateWiseHandlerMap[state]!!.insertCoin(coin)
    }

    fun selectSlot(slotId: SlotId) {
        stateWiseHandlerMap[state]!!.selectSlot(slotId)
    }

    fun restock(quantityMap: Map<SlotId, Int>) {
        stateWiseHandlerMap[state]!!.restock(quantityMap)
    }

    fun cancel(): Map<Coin, Int> {
        return stateWiseHandlerMap[state]!!.cancel()
    }

    fun dispense(): Purchase {
        return stateWiseHandlerMap[state]!!.dispense()
    }

    internal fun moveTo(state: VendingMachineState) {
        this.state = state
    }


}
