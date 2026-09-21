package lld.parkingbooking.entity

import kotlin.time.Instant

data class TimeInterval(
    val start: Instant,
    val end: Instant
) {
    init {
        require(start < end) { "start must be before end" }
    }

    fun overlaps(other: TimeInterval): Boolean {
        return start < other.end && other.start < end
    }
}
