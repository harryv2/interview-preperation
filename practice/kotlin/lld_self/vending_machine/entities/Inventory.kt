package lld_self.vending_machine.entities


class Inventory(
    initialInventory: List<VendingSlot>
) {

    private var slotMap = HashMap<SlotId, VendingSlot>()

    init {
        initialInventory.forEach { it ->
            slotMap[it.id] = it
        }
    }

    fun add(slot: VendingSlot) {
        slotMap[slot.id] = slot
    }

    fun find(slotId: SlotId): VendingSlot? {
        return slotMap[slotId]
    }

    fun remove(slotId: SlotId) {
        val slot = slotMap[slotId]
        checkNotNull(slot) { "No slot $slotId" }
        check(!slot.isEmpty()) { "Slot $slotId is empty" }
        slot.remove()
    }

    fun restock(slotId: SlotId, quantity: Int) {
        val slot = slotMap[slotId]
        checkNotNull(slot) { "No slot $slotId" }
        slot.add(quantity)
    }

    fun isCompletelyEmpty(): Boolean {
        return slotMap.all { it.value.isEmpty() }
    }
}