package lld_self.bookmyshow.entities

import kotlin.time.Instant
import kotlin.uuid.Uuid

enum class ShowSeatState {
    AVAILABLE,
    LOCKED,
    BOOKED
}


class ShowSeat(
    val seat: Seat,
    val price: Money
) {
    val id = Uuid.random()

    var state = ShowSeatState.AVAILABLE


    fun isFree(): Boolean = state == ShowSeatState.AVAILABLE

    var lockedBy: Uuid? = null
    var expiresAt: Instant? = null

    fun lock(booking: Uuid, until: Instant) {
        require(state == ShowSeatState.AVAILABLE) { "Seat is not available" }
        state = ShowSeatState.LOCKED
        expiresAt = until
        lockedBy = booking
    }

    fun confirm(booking: Booking) {
        require(lockedBy == booking.id) {"Booking mismatch"}
        state = ShowSeatState.BOOKED
    }

    fun unlock(booking: Booking) {
        require(lockedBy == booking.id) {"Booking mismatch"}
        state = ShowSeatState.AVAILABLE
        expiresAt = null
        lockedBy = null
    }

}