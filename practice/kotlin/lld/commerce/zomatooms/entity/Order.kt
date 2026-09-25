package lld.commerce.zomatooms.entity


enum class OrderStatus {
    PLACED,
    ACCEPTED,
    READY,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED,
    REJECTED;

    fun canMoveTo(next: OrderStatus): Boolean {
        return next in ALLOWED[this].orEmpty()
    }

    companion object {
        private val ALLOWED = mapOf(
            PLACED to setOf(ACCEPTED, REJECTED, CANCELLED),
            ACCEPTED to setOf(READY, CANCELLED),
            READY to setOf(OUT_FOR_DELIVERY, CANCELLED),
            OUT_FOR_DELIVERY to setOf(DELIVERED)
        )
    }
}


data class OrderItem(
    val item: MenuItem,
    val quantity: Int
) {
    val lineTotal: Money
        get() = item.price * quantity
}


data class Bill(
    val itemTotal: Money,
    val packagingFee: Money,
    val deliveryFee: Money,
    val tax: Money,
    val discount: Money
) {
    val payable: Money
        get() = itemTotal + packagingFee + deliveryFee + tax - discount

    override fun toString(): String {
        return "items $itemTotal + packaging $packagingFee + delivery $deliveryFee + tax $tax - discount $discount = $payable"
    }
}


class Order(
    val id: String,
    val customerId: String,
    val restaurantId: String,
    val items: List<OrderItem>,
    val bill: Bill
) {

    var status: OrderStatus = OrderStatus.PLACED
        private set

    var deliveryPartnerId: String? = null
        private set

    fun moveTo(next: OrderStatus) {
        require(status.canMoveTo(next)) { "Order $id can not move from $status to $next" }
        status = next
    }

    fun assignTo(partnerId: String) {
        check(deliveryPartnerId == null) { "Order $id already has a partner" }
        deliveryPartnerId = partnerId
    }

    fun releasePartner(): String? {
        val partnerId = deliveryPartnerId
        deliveryPartnerId = null
        return partnerId
    }

    override fun toString(): String {
        return "Order $id [$status] ${items.joinToString { "${it.quantity} x ${it.item.name}" }} -> ${bill.payable}"
    }
}
