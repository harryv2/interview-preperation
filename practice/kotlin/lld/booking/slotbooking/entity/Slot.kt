package lld.booking.slotbooking.entity

import java.time.LocalDateTime

data class Slot(
    val id: SlotId,
    val zone: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val capacity: Int,
    val booked: Int
) {
    val remaining: Int
        get() = capacity - booked
}
