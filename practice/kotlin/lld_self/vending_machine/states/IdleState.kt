package lld_self.vending_machine.states

import lld_self.vending_machine.VendingMachine
import lld_self.vending_machine.entities.Coin
import lld_self.vending_machine.entities.SlotId


class IdleState(
    private val vendingMachine: VendingMachine
) : VendingMachineStateHandler {

    override val state = VendingMachineState.IDLE

    override fun insertCoin(coin: Coin) {
        vendingMachine.holdCoin(coin)
        vendingMachine.moveTo(VendingMachineState.COIN_INSERTED)
    }

    override fun cancel(): Map<Coin, Int> {
        return emptyMap()
    }

    override fun restock(quantityMap: Map<SlotId, Int>) {
        vendingMachine.applyRestock(quantityMap)
    }
}
