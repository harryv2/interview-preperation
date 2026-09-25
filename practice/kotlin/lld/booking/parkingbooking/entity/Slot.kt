package lld.booking.parkingbooking.entity

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

enum class SlotType {
    BIKE,
    CAR,
    TRUCK
}

class Slot(
    val id: String,
    val type: SlotType
) {
    private val bookings = mutableListOf<Booking>()
    private val lock = ReentrantLock()

    fun isAvailable(interval: TimeInterval): Boolean {
        lock.withLock {
            return bookings.none { it.interval.overlaps(interval) }
        }
    }

    fun tryBook(booking: Booking): Boolean {
        lock.withLock {
            if (!isAvailable(booking.interval)) {
                return false
            }

            bookings.add(booking)
            return true
        }
    }

    fun release(booking: Booking) {
        lock.withLock {
            bookings.remove(booking)
        }
    }
}
