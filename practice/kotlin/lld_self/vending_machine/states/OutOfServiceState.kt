package lld_self.vending_machine.states

import lld_self.vending_machine.VendingMachine
import lld_self.vending_machine.entities.SlotId


class OutOfServiceState(
    private val vendingMachine: VendingMachine
) : VendingMachineStateHandler {

    override val state = VendingMachineState.OUT_OF_SERVICE

    override fun restock(quantityMap: Map<SlotId, Int>) {
        vendingMachine.applyRestock(quantityMap)

        if (!vendingMachine.isSoldOut()) {
            vendingMachine.moveTo(VendingMachineState.IDLE)
        }
    }
}
