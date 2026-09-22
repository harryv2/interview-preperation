package lld.deliveryslot.entity

import java.time.LocalDateTime

data class TimeSlot(
    val id: String,
    val warehouseId: String,
    val start: LocalDateTime,
    val end: LocalDateTime
)
