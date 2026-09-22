package lld_self.vending_machine.entities


data class Purchase(
    val totalPaid: Money,
    val product: Product,
    val change: Map<Coin, Int>
) {

    override fun toString(): String {
        return "Purchase -> paid: $totalPaid,  product: $product, change: $change"
    }
}
