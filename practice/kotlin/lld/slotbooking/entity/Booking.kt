package lld.slotbooking.entity

import java.time.Instant

enum class BookingStatus {
    CONFIRMED,
    CANCELLED
}

data class Booking(
    val id: BookingId,
    val orderId: OrderId,
    val slotId: SlotId,
    val status: BookingStatus,
    val createdAt: Instant
)
