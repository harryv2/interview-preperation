package lld.booking.restaurantreservation.entity

import java.time.LocalDateTime

data class TimeInterval(
    val start: LocalDateTime,
    val end: LocalDateTime
) {
    init {
        require(start < end) { "start must be before end" }
    }

    fun overlaps(other: TimeInterval): Boolean {
        return start < other.end && other.start < end
    }
}
