package lld_self.vending_machine.states

import lld_self.vending_machine.VendingMachine
import lld_self.vending_machine.entities.Coin
import lld_self.vending_machine.entities.Purchase


class DispensingState(
    private val vendingMachine: VendingMachine
) : VendingMachineStateHandler {

    override val state = VendingMachineState.DISPENSING

    override fun dispense(): Purchase {
        val purchase = vendingMachine.completeSale()
        vendingMachine.moveTo(nextState())
        return purchase
    }

    override fun cancel(): Map<Coin, Int> {
        val refund = vendingMachine.abandonSale()
        vendingMachine.moveTo(VendingMachineState.IDLE)
        return refund
    }

    private fun nextState(): VendingMachineState {
        if (vendingMachine.isSoldOut()) {
            return VendingMachineState.OUT_OF_SERVICE
        }
        return VendingMachineState.IDLE
    }
}
