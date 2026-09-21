package lld.parkingbooking.entity

import kotlin.uuid.Uuid

enum class BookingStatus {
    CONFIRMED,
    CANCELLED
}

class Booking(
    val id: Uuid,
    val company: Company,
    val slot: Slot,
    val interval: TimeInterval
) {
    var status: BookingStatus = BookingStatus.CONFIRMED
        private set

    fun cancel() {
        status = BookingStatus.CANCELLED
    }
}
