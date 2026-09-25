package lld.fintech.orderbook.entity


enum class Side {
    BUY,
    SELL;

    fun opposite(): Side {
        return if (this == BUY) SELL else BUY
    }
}


enum class OrderType {
    LIMIT,
    MARKET
}


enum class OrderStatus {
    NEW,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED
}


class Order(
    val id: String,
    val trader: String,
    val side: Side,
    val type: OrderType,
    val limitPrice: Price?,
    val quantity: Long,
    val sequence: Long
) {

    init {
        require(quantity > 0) { "Quantity must be positive" }
        if (type == OrderType.LIMIT) {
            requireNotNull(limitPrice) { "A limit order needs a price" }
        }
    }

    var filled: Long = 0
        private set

    var status: OrderStatus = OrderStatus.NEW
        private set

    val remaining: Long
        get() = quantity - filled

    fun fill(amount: Long) {
        require(amount in 1..remaining) { "Can not fill $amount of $remaining on order $id" }
        filled += amount
        status = if (remaining == 0L) OrderStatus.FILLED else OrderStatus.PARTIALLY_FILLED
    }

    fun cancel() {
        check(status != OrderStatus.FILLED) { "Order $id is already filled" }
        status = OrderStatus.CANCELLED
    }

    fun crossesWith(restingPrice: Price): Boolean {
        if (type == OrderType.MARKET) {
            return true
        }
        return if (side == Side.BUY) {
            restingPrice <= limitPrice!!
        } else {
            restingPrice >= limitPrice!!
        }
    }

    override fun toString(): String {
        val price = limitPrice?.toString() ?: "MKT"
        return "$id $side $quantity @ $price [$status ${filled}/${quantity}]"
    }
}
