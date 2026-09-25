package lld.booking.deliveryslot.entity

enum class BookingStatus {
    CONFIRMED,
    CANCELLED
}

data class Booking(
    val id: String,
    val orderId: String,
    val slotId: String,
    val vanId: String,
    val warehouseId: String,
    val status: BookingStatus
)
