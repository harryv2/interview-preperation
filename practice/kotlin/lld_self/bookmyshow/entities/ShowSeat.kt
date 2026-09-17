package lld_self.bookmyshow.entities

import kotlin.time.Clock
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
        private set
    var lockedBy: Uuid? = null
        private set
    var expiresAt: Instant? = null
        private set

    // lazy expiry: a LOCKED seat whose lock has run out counts as free
    fun isFree(now: Instant = Clock.System.now()): Boolean {
        if (state == ShowSeatState.AVAILABLE) return true
        if (state == ShowSeatState.LOCKED && expiresAt!! < now) return true
        return false
    }

    fun lock(bookingId: Uuid, until: Instant) {
        require(isFree()) { "Seat ${seat.name} is not available" }
        state = ShowSeatState.LOCKED
        expiresAt = until
        lockedBy = bookingId
    }

    fun confirm(booking: Booking) {
        check(state == ShowSeatState.LOCKED) { "Seat ${seat.name} is not locked" }
        check(lockedBy == booking.id) { "Booking mismatch" }
        state = ShowSeatState.BOOKED
        expiresAt = null
    }

    fun unlock(booking: Booking) {
        if (state != ShowSeatState.LOCKED) return
        if (lockedBy != booking.id) return
        state = ShowSeatState.AVAILABLE
        expiresAt = null
        lockedBy = null
    }

    override fun toString() = "${seat.name}[$state]"
}
