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
