package lld_self.vending_machine.entities


@JvmInline
value class SlotId(val id: String)


class VendingSlot(
    val id: SlotId,
    val product: Product,
    private var quantity: Int
) {

    fun add(amount: Int) {
        require(amount > 0) { "Amount must be positive" }
        quantity += amount
    }

    fun isEmpty(): Boolean = quantity == 0

    fun remove() {
        check(quantity > 0) { "Slot $id is empty" }
        quantity -= 1
    }
}
