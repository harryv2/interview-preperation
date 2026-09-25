package lld.carrental.entity

import java.time.Duration
import java.time.LocalDateTime


data class TimeRange(
    val start: LocalDateTime,
    val end: LocalDateTime
) {

    init {
        require(start < end) { "start must be before end" }
    }

    fun overlaps(other: TimeRange): Boolean {
        return start < other.end && other.start < end
    }

    fun days(): Long {
        val hours = Duration.between(start, end).toHours()
        return (hours + 23) / 24
    }
}
