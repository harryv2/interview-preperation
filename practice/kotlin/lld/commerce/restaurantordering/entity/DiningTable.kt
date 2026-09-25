package lld.commerce.restaurantordering.entity


enum class TableStatus {
    FREE,
    OCCUPIED
}

class DiningTable(
    val number: String,
    val seats: Int
) {

    var status: TableStatus = TableStatus.FREE
        private set

    var activeOrderId: String? = null
        private set

    fun occupy(orderId: String) {
        check(status == TableStatus.FREE) { "Table $number is already serving $activeOrderId" }
        status = TableStatus.OCCUPIED
        activeOrderId = orderId
    }

    fun free() {
        status = TableStatus.FREE
        activeOrderId = null
    }

    override fun toString(): String {
        return "Table $number ($seats seats) $status"
    }
}
