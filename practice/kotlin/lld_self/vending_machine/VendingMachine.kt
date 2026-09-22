package lld_self.vending_machine

import lld_self.vending_machine.entities.Coin
import lld_self.vending_machine.entities.CoinBank
import lld_self.vending_machine.entities.Inventory
import lld_self.vending_machine.entities.Money
import lld_self.vending_machine.entities.Purchase
import lld_self.vending_machine.entities.Sale
import lld_self.vending_machine.entities.SlotId
import lld_self.vending_machine.entities.VendingSlot
import lld_self.vending_machine.exceptions.ChangeUnavailableException
import lld_self.vending_machine.exceptions.InsufficientFundsException
import lld_self.vending_machine.exceptions.SlotEmptyException
import lld_self.vending_machine.exceptions.SlotNotFoundException
import lld_self.vending_machine.states.CoinInsertedState
import lld_self.vending_machine.states.DispensingState
import lld_self.vending_machine.states.IdleState
import lld_self.vending_machine.states.OutOfServiceState
import lld_self.vending_machine.states.VendingMachineState
import lld_self.vending_machine.states.VendingMachineStateHandler
import lld_self.vending_machine.strategies.ChangeMakerStrategy


class VendingMachine(
    val id: String,
    coins: Map<Coin, Int>,
    initialInventory: List<VendingSlot>,
    changeMakerStrategy: ChangeMakerStrategy
) {

    private val bank = CoinBank(coins, changeMakerStrategy)
    private val inventory = Inventory(initialInventory)

    private var sale = Sale()

    private val handlers = mapOf(
        VendingMachineState.IDLE to IdleState(this),
        VendingMachineState.COIN_INSERTED to CoinInsertedState(this),
        VendingMachineState.DISPENSING to DispensingState(this),
        VendingMachineState.OUT_OF_SERVICE to OutOfServiceState(this)
    )

    private var state = if (inventory.isSoldOut()) {
        VendingMachineState.OUT_OF_SERVICE
    } else {
        VendingMachineState.IDLE
    }

    private val handler: VendingMachineStateHandler
        get() = handlers.getValue(state)


    fun insertedAmount(): Money {
        return sale.insertedAmount
    }

    fun insertCoin(coin: Coin) {
        handler.insertCoin(coin)
    }

    fun selectSlot(slotId: SlotId) {
        handler.selectSlot(slotId)
    }

    fun dispense(): Purchase {
        return handler.dispense()
    }

    fun cancel(): Map<Coin, Int> {
        return handler.cancel()
    }

    fun restock(quantityMap: Map<SlotId, Int>) {
        handler.restock(quantityMap)
    }


    internal fun holdCoin(coin: Coin) {
        sale.addCoin(coin)
    }

    internal fun reserve(slotId: SlotId) {
        val slot = inventory.find(slotId) ?: throw SlotNotFoundException(slotId)

        if (slot.isEmpty()) {
            throw SlotEmptyException(slotId)
        }

        val inserted = sale.insertedAmount
        if (inserted < slot.product.price) {
            throw InsufficientFundsException(slot.product.price, inserted)
        }

        val changeDue = inserted - slot.product.price
        if (!bank.canSettle(sale.insertedCoins(), changeDue)) {
            throw ChangeUnavailableException(changeDue)
        }

        sale.select(slotId)
    }

    internal fun completeSale(): Purchase {
        val slotId = sale.selectedSlotId
        checkNotNull(slotId) { "No slot selected" }

        val slot = inventory.find(slotId) ?: throw SlotNotFoundException(slotId)

        val paid = sale.insertedAmount
        val change = bank.settle(sale.insertedCoins(), paid - slot.product.price)
        inventory.dispenseOne(slotId)
        sale = Sale()

        return Purchase(paid, slot.product, change)
    }

    internal fun abandonSale(): Map<Coin, Int> {
        val refund = sale.refund()
        sale = Sale()
        return refund
    }

    internal fun applyRestock(quantityMap: Map<SlotId, Int>) {
        quantityMap.forEach { (slotId, count) ->
            inventory.restock(slotId, count)
        }
    }

    internal fun isSoldOut(): Boolean {
        return inventory.isSoldOut()
    }

    internal fun moveTo(state: VendingMachineState) {
        this.state = state
    }
}
