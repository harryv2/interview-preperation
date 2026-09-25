package lld.orderbook.entity


data class Trade(
    val buyOrderId: String,
    val sellOrderId: String,
    val price: Price,
    val quantity: Long,
    val sequence: Long
) {

    override fun toString(): String {
        return "TRADE $quantity @ $price  (buy $buyOrderId / sell $sellOrderId)"
    }
}


data class BookLevel(
    val price: Price,
    val quantity: Long,
    val orders: Int
) {
    override fun toString(): String {
        return "$price x $quantity ($orders)"
    }
}
