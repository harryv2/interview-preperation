package lld.restaurantordering.entity


class Bill(
    val orderId: String,
    val subtotal: Money,
    val discount: Money,
    val serviceCharge: Money,
    val tax: Money
) {

    val total: Money = subtotal - discount + serviceCharge + tax

    override fun toString(): String {
        return """
            Bill for order $orderId
              subtotal        $subtotal
              discount        -$discount
              service charge  $serviceCharge
              tax             $tax
              total           $total
        """.trimIndent()
    }
}
