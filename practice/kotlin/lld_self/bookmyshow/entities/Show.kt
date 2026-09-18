package lld_self.bookmyshow.entities

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid


class Show(
    val movie: Movie,
    val screen: Screen,
    val startTime: Instant
) {
    val id = Uuid.random()
    val endTime = startTime + movie.duration

    private val lock = ReentrantLock()

    private val allSeats: Map<Uuid, ShowSeat> = screen.seats.associate {
        it.id to ShowSeat(it, it.basePrice)
    }

    fun getAvailableSeats(): List<ShowSeat> {
        lock.withLock {
            val now = Clock.System.now()
            return allSeats.values.filter { it.isFree(now) }
        }
    }

    fun reserveSeats(seatIds: List<Uuid>, bookingId: Uuid, ttl: Duration): List<ShowSeat> {
        require(seatIds.isNotEmpty()) { "No seats selected" }
        require(seatIds.distinct().size == seatIds.size) { "Duplicate seats selected" }

        lock.withLock {
            val now = Clock.System.now()

            seatIds.forEach {
                require(allSeats.contains(it)) { "Seat $it does not belong to show $id" }
                require(allSeats[it]!!.isFree(now)) { "Seat ${allSeats[it]!!.seat.name} is taken" }
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

    fun releaseSeats(booking: Booking) {
        lock.withLock {
            booking.showSeats.forEach {
                it.unlock(booking)
            }
        }
    }

    fun overlaps(other: Show): Boolean {
        if (screen.id != other.screen.id) return false
        return startTime < other.endTime + CLEANUP_BUFFER && other.startTime < endTime + CLEANUP_BUFFER
    }

    override fun toString() = "${movie.name} | ${screen.name} | $startTime"

    companion object {
        private val CLEANUP_BUFFER = 30.minutes
    }
}
