package lld_self.vending_machine.entities

import lld_self.vending_machine.exceptions.SlotEmptyException
import lld_self.vending_machine.exceptions.SlotNotFoundException


class Inventory(
    initialInventory: List<VendingSlot>
) {

    private val slotMap = LinkedHashMap<SlotId, VendingSlot>()

    init {
        initialInventory.forEach {
            slotMap[it.id] = it
        }
    }

    fun add(slot: VendingSlot) {
        slotMap[slot.id] = slot
    }

    fun find(slotId: SlotId): VendingSlot? {
        return slotMap[slotId]
    }

    fun dispenseOne(slotId: SlotId) {
        val slot = slotMap[slotId] ?: throw SlotNotFoundException(slotId)
        if (slot.isEmpty()) {
            throw SlotEmptyException(slotId)
        }
        slot.remove()
    }

    fun restock(slotId: SlotId, quantity: Int) {
        val slot = slotMap[slotId] ?: throw SlotNotFoundException(slotId)
        slot.add(quantity)
    }

    fun isSoldOut(): Boolean {
        return slotMap.all { it.value.isEmpty() }
    }
}
