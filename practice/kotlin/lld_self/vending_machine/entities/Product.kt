package lld_self.vending_machine.entities




data class Product(
    val id: String,
    val name: String,
    val price: Money
) {

    override fun toString(): String {
        return "Product: $name of $price"
    }

}


data class Purchase(
    val totalPaid: Money,
    val product: Product,
    val change: Map<Coin, Int>
) {

    override fun toString(): String {
        return "Purchase -> paid: $totalPaid,  product: $product, change: $change"
    }
}