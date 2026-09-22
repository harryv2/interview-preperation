package lld_self.vending_machine.states

import lld_self.vending_machine.entities.Coin
import lld_self.vending_machine.entities.Purchase
import lld_self.vending_machine.entities.SlotId
import lld_self.vending_machine.exceptions.ActionNotAllowedException


enum class VendingMachineState {
    IDLE,
    COIN_INSERTED,
    DISPENSING,
    OUT_OF_SERVICE
}

interface VendingMachineStateHandler {

    val state: VendingMachineState

    fun insertCoin(coin: Coin) {
        throw ActionNotAllowedException("Inserting a coin", state.name)
    }

    fun selectSlot(slotId: SlotId) {
        throw ActionNotAllowedException("Selecting a slot", state.name)
    }

    fun dispense(): Purchase {
        throw ActionNotAllowedException("Dispensing", state.name)
    }

    fun cancel(): Map<Coin, Int> {
        throw ActionNotAllowedException("Cancelling", state.name)
    }

    fun restock(quantityMap: Map<SlotId, Int>) {
        throw ActionNotAllowedException("Restocking", state.name)
    }
}
