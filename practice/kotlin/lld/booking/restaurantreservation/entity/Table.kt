package lld.booking.restaurantreservation.entity

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Table(
    val id: String,
    val seats: Int
) {
    private val reservations = mutableListOf<Reservation>()
    private val lock = ReentrantLock()

    fun fits(partySize: Int): Boolean {
        return partySize in 1..seats
    }

    fun isAvailable(interval: TimeInterval): Boolean {
        lock.withLock {
            return reservations.none { it.interval.overlaps(interval) }
        }
    }

    fun tryReserve(reservation: Reservation): Boolean {
        lock.withLock {
            if (!isAvailable(reservation.interval)) {
                return false
            }

            reservations.add(reservation)
            return true
        }
    }

    fun release(reservation: Reservation) {
        lock.withLock {
            reservations.remove(reservation)
        }
    }
}
