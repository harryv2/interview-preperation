package lld_self.bookmyshow.entities

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid


class Show(
    val movie: Movie,
    val screen: Screen,
    val starTime: Instant
) {
    val id = Uuid.random()
    val lock = ReentrantLock()

    val allSeats = screen.seats.associate {
        Pair(it.id, ShowSeat(it, it.basePrice))
    }

    fun getAvailableSeats(): List<ShowSeat> {
        return allSeats.values.filter { it.isFree() }
    }

    fun reserveSeats(seatIds: List<Uuid>, bookingId: Uuid, ttl: Duration): List<ShowSeat> {
        lock.withLock {
            val now = Clock.System.now()

            seatIds.forEach {
                require(allSeats.contains(it)) { "Seat id wrong" }
                require(allSeats[it]!!.isFree()) {"Seat ${allSeats[it]!!.seat.name} is taken"}
            }

            val showSeats = seatIds.map { allSeats[it]!! }
            showSeats.forEach {
                it.lock(bookingId, now + ttl)
            }
            return showSeats
        }
    }

    fun confirmSeats(booking: Booking) {
        lock.withLock {
            booking.showSeats.forEach {
                it.confirm(booking)
            }
        }
    }

    fun releaseSeats(booking: Booking){
        lock.withLock {
            booking.showSeats.forEach {
                it.unlock(booking)
            }
        }
    }
}