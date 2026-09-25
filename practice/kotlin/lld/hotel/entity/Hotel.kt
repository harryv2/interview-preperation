package lld.hotel.entity

import lld.hotel.strategy.PricingPolicy
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


// The aggregate root. Bookings are counted against a type, rooms are handed out at check in, and both sides
// live here behind one lock because booking is a check then act.
class Hotel(
    val id: String,
    val name: String,
    val city: String,
    private val rooms: List<Room>,
    private val pricing: PricingPolicy,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    private val lock = ReentrantLock()
    private val roomsByType = rooms.groupBy { it.type }
    private val reservations = mutableMapOf<String, Reservation>()

    fun rooms(): List<Room> = rooms.sortedBy { it.number }

    // how many rooms of this type are still sellable for every night of the stay. The binding night is the
    // busiest one, so it is the peak overlap across the range and not a sum.
    fun freeRooms(type: RoomType, stay: DateRange): Int {
        lock.withLock {
            val committed = reservations.values.filter { it.holdsInventory() && it.type == type }
            val peak = stay.dates().maxOf { night -> committed.count { it.stay.covers(night) } }
            return roomsByType[type].orEmpty().size - peak
        }
    }

    fun availability(stay: DateRange): Map<RoomType, Int> {
        lock.withLock {
            return RoomType.values().associateWith { freeRooms(it, stay) }
        }
    }

    fun quote(type: RoomType, stay: DateRange): Money {
        return pricing.quote(type, stay)
    }

    // the guest picks a type and a date range, never a room number
    fun book(guest: Guest, type: RoomType, stay: DateRange): Reservation {
        lock.withLock {
            require(!stay.checkIn.isBefore(today())) { "Stay $stay starts in the past" }
            require(freeRooms(type, stay) > 0) { "No $type free for $stay" }

            val reservation = Reservation(newId(), id, guest, type, stay, pricing.quote(type, stay))
            reservations[reservation.id] = reservation
            return reservation
        }
    }

    // First fit, and it can never get stuck. Guests arrive in check in date order, and first fit by start time
    // colours an interval graph in exactly peak-overlap many rooms. The booking rule caps peak overlap at the
    // number of rooms of that type, so a free room is always there.
    fun checkIn(reservationId: String): Reservation {
        lock.withLock {
            val reservation = reservation(reservationId)
            val free = roomsByType[reservation.type].orEmpty().firstOrNull { it.isFree(reservation.stay) }
            checkNotNull(free) { "No ${reservation.type} free to hand over, the inventory count is wrong" }

            reservation.checkIn(free, today())
            free.hold(reservation.id, reservation.stay)
            return reservation
        }
    }

    fun checkOut(reservationId: String): Reservation {
        lock.withLock {
            val reservation = reservation(reservationId)
            reservation.checkOut()
            return reservation
        }
    }

    // before check in there is no room to give back, the type's inventory frees up on its own
    fun cancel(reservationId: String): Reservation {
        lock.withLock {
            val reservation = reservation(reservationId)
            reservation.cancel()
            return reservation
        }
    }

    fun reservationsOf(guest: Guest): List<Reservation> {
        lock.withLock {
            return reservations.values.filter { it.guest.id == guest.id }.sortedBy { it.stay.checkIn }
        }
    }

    private fun reservation(reservationId: String): Reservation {
        return requireNotNull(reservations[reservationId]) { "No reservation $reservationId" }
    }

    private fun today(): LocalDate {
        return LocalDate.now(clock)
    }

    // prefixed with the property, so six characters stay unique once more than one hotel is issuing ids
    private fun newId(): String {
        return "$id-${UUID.randomUUID().toString().take(6)}"
    }

    override fun toString(): String {
        return "$name, $city (${rooms.size} rooms)"
    }
}
