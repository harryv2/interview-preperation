package lld_self.vending_machine.entities


@JvmInline
value class SlotId(val id: String)


class VendingSlot(
    val id: SlotId,
    val product: Product,
    private var quantity: Int
) {

    fun add(amount: Int) {
        quantity += quantity
    }

    fun isEmpty(): Boolean = quantity == 0

    fun remove() {
        quantity -= 1
    }
}