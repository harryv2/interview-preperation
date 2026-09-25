package lld.booking.meetingscheduler.entity

import kotlin.time.Duration
import kotlin.time.Instant

data class TimeSlot(val start: Instant, val end: Instant) {
    init {
        require(start < end) { "Slot must end after it starts" }
    }

    val duration: Duration
        get() = end - start

    fun overlaps(other: TimeSlot): Boolean {
        return start < other.end && other.start < end
    }
}
