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
        vendingMachine.coinBox.addToHeld(coin)
        vendingMachine.moveTo(VendingMachineState.COIN_INSERTED)
    }

    override fun cancel(): Map<Coin, Int> {
        return emptyMap()
    }

    override fun restock(quantityMap: Map<SlotId, Int>) {
        quantityMap.forEach { (id, count) ->
            vendingMachine.inventory.restock(id, count)
        }
    }

}


class CoinInsertedState(
    private val vendingMachine: VendingMachine
) : VendingMachineStateHandler {

    override fun insertCoin(coin: Coin) {
        vendingMachine.coinBox.addToHeld(coin)
    }

    override fun selectSlot(slotId: SlotId) {
        val slot = vendingMachine.inventory.find(slotId)
        check(slot != null) { "Invalid slotId $slotId" }

        require(!slot.isEmpty()) { "Slot is empty" }

        val heldAmount = vendingMachine.coinBox.heldAmount
        require(slot.product.price <= heldAmount) { "Please add more money" }

        val changeDue = heldAmount - slot.product.price
        require(vendingMachine.coinBox.canMakeChange(changeDue)) { "Exact change not available" }

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
        val slotId = vendingMachine.selectedSlotId
        require(slotId != null) { "Slot not selected" }

        val slot = vendingMachine.inventory.find(slotId)
        check(slot != null) { "Invalid slotId $slotId" }

        val heldAmount = vendingMachine.coinBox.heldAmount

        val change = vendingMachine.coinBox.settleUp(slot.product.price)
        vendingMachine.inventory.remove(slotId)

        vendingMachine.selectedSlotId = null
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

        if (!vendingMachine.inventory.isCompletelyEmpty()) {
            vendingMachine.moveTo(VendingMachineState.IDLE)
        }
    }
}
