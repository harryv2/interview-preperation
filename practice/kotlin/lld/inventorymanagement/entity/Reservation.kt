package lld.inventorymanagement.entity

enum class ReservationStatus {
    HELD,
    CONFIRMED,
    RELEASED
}

class Reservation(
    val id: String,
    val orderId: String,
    val sku: String,
    val warehouseId: String,
    val quantity: Int
) {
    var status: ReservationStatus = ReservationStatus.HELD
        private set

    fun confirm() {
        check(status == ReservationStatus.HELD) { "Reservation $id is $status" }
        status = ReservationStatus.CONFIRMED
    }

    fun release() {
        check(status == ReservationStatus.HELD) { "Reservation $id is $status" }
        status = ReservationStatus.RELEASED
    }
}
