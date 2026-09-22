package lld_self.vending_machine.states

import lld_self.vending_machine.VendingMachine
import lld_self.vending_machine.entities.Coin
import lld_self.vending_machine.entities.SlotId


class CoinInsertedState(
    private val vendingMachine: VendingMachine
) : VendingMachineStateHandler {

    override val state = VendingMachineState.COIN_INSERTED

    override fun insertCoin(coin: Coin) {
        vendingMachine.holdCoin(coin)
    }

    override fun selectSlot(slotId: SlotId) {
        vendingMachine.reserve(slotId)
        vendingMachine.moveTo(VendingMachineState.DISPENSING)
    }

    override fun cancel(): Map<Coin, Int> {
        val refund = vendingMachine.abandonSale()
        vendingMachine.moveTo(VendingMachineState.IDLE)
        return refund
    }
}
