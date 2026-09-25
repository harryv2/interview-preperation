package lld.booking.hotel.entity

import java.time.LocalDate
import java.time.temporal.ChronoUnit


// Half open on purpose. A stay occupies the NIGHTS of checkIn up to checkOut - 1, so the guest leaving on the
// 13th and the guest arriving on the 13th do not collide. Getting this wrong is the classic hotel booking bug.
class DateRange(
    val checkIn: LocalDate,
    val checkOut: LocalDate
) {

    init {
        require(checkOut.isAfter(checkIn)) { "Check out $checkOut must be after check in $checkIn" }
    }

    val nights: Int
        get() = ChronoUnit.DAYS.between(checkIn, checkOut).toInt()

    fun overlaps(other: DateRange): Boolean {
        return checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(checkOut)
    }

    fun covers(night: LocalDate): Boolean {
        return !night.isBefore(checkIn) && night.isBefore(checkOut)
    }

    fun dates(): List<LocalDate> {
        return (0 until nights).map { checkIn.plusDays(it.toLong()) }
    }

    override fun toString(): String {
        return "$checkIn to $checkOut ($nights nights)"
    }
}
