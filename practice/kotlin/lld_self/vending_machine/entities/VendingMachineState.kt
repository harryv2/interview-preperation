package lld_self.vending_machine.entities


class ActionNotAllowedException(message: String) : RuntimeException(message)

interface VendingMachineStateHandler {
    fun insertCoin(coin: Coin) {
        throw ActionNotAllowedException("Not allowed")
    }

    fun selectSlot(slotId: SlotId) {
        throw ActionNotAllowedException("Not allowed")
    }

    fun dispense(): Purchase {
        throw ActionNotAllowedException("Not allowed")
    }

    fun cancel(): Map<Coin, Int> {
        throw ActionNotAllowedException("Not allowed")
    }

    fun restock(quantityMap: Map<SlotId, Int>) {
        throw ActionNotAllowedException("Not allowed")
    }
}


class IdleState(
    private val vendingMachine: VendingMachine
) : VendingMachineStateHandler {

    override fun insertCoin(coin: Coin) {
        vendingMachine.coinBox.addToHelp(coin)
        vendingMachine.moveTo(VendingMachineState.COIN_INSERTED)
    }

}


class CoinInsertedState(
    private val vendingMachine: VendingMachine
) : VendingMachineStateHandler {

    override fun insertCoin(coin: Coin) {
        vendingMachine.coinBox.addToHelp(coin)
    }

    override fun selectSlot(slotId: SlotId) {
        val slot = vendingMachine.inventory.find(slotId)
        check(slot != null) { "Invalid slotId $slotId" }

        require(slot.product.price <= vendingMachine.coinBox.heldAmount) { "Please add more money" }

        require(!slot.isEmpty()) { "Slot is empty" }

        vendingMachine.coinBox.canMakeChange(slot.product.price)

        vendingMachine.selectedSlotId = slotId
        vendingMachine.moveTo(VendingMachineState.DISPENSING)
    }

    override fun cancel(): Map<Coin, Int> {
        vendingMachine.selectedSlotId = null
        vendingMachine.moveTo(VendingMachineState.IDLE)
        return vendingMachine.coinBox.refund()
    }
}

class DispensingState(
    private val vendingMachine: VendingMachine
) : VendingMachineStateHandler {

    override fun cancel(): Map<Coin, Int> {
        vendingMachine.selectedSlotId = null
        vendingMachine.moveTo(VendingMachineState.IDLE)
        return vendingMachine.coinBox.refund()
    }

    override fun dispense(): Purchase {
        require(vendingMachine.selectedSlotId != null) { "Slot not selected" }
        vendingMachine.inventory.remove(vendingMachine.selectedSlotId!!)

        val slot = vendingMachine.inventory.find(vendingMachine.selectedSlotId!!)
        check(slot != null) { "Invalid slotId $vendingMachine.selectedSlotId" }

        val heldAmount = vendingMachine.coinBox.heldAmount

        val change = vendingMachine.coinBox.settleUp(slot.product.price)

        if (vendingMachine.inventory.isCompletelyEmpty()) {
            vendingMachine.moveTo(VendingMachineState.OUT_OF_SERVICE)
        } else {
            vendingMachine.moveTo(VendingMachineState.IDLE)
        }

        return Purchase(
            heldAmount,
            slot.product,
            change
        )
    }
}

class OutOfServiceState(
    private val vendingMachine: VendingMachine
) : VendingMachineStateHandler {

    override fun restock(quantityMap: Map<SlotId, Int>) {
        quantityMap.forEach { (id, count) ->
            vendingMachine.inventory.restock(id, count)
        }
    }
}