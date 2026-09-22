package lld.restaurantreservation.service

import lld.restaurantreservation.entity.Customer
import lld.restaurantreservation.entity.Reservation
import lld.restaurantreservation.entity.Table
import lld.restaurantreservation.entity.TimeInterval
import lld.restaurantreservation.strategy.TableSelectionStrategy
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class Restaurant(
    val name: String,
    private val tables: List<Table>,
    private val strategy: TableSelectionStrategy,
    private val opensAt: LocalTime = LocalTime.of(11, 0),
    private val closesAt: LocalTime = LocalTime.of(23, 0),
    private val slotDuration: Duration = Duration.ofHours(2)
) {
    private val reservations = ConcurrentHashMap<String, Reservation>()

    fun availableTables(partySize: Int, start: LocalDateTime): List<Table> {
        return strategy.candidates(tables, partySize, intervalFrom(start))
    }

    fun reserve(customer: Customer, partySize: Int, start: LocalDateTime): Reservation? {
        val interval = intervalFrom(start)

        for (table in strategy.candidates(tables, partySize, interval)) {
            val reservation = Reservation(UUID.randomUUID().toString(), customer, table, partySize, interval)

            if (table.tryReserve(reservation)) {
                reservations[reservation.id] = reservation
                return reservation
            }
        }

        return null
    }

    fun checkIn(reservationId: String): Reservation {
        val reservation = reservation(reservationId)
        reservation.seat()
        return reservation
    }

    fun complete(reservationId: String): Reservation {
        val reservation = reservation(reservationId)
        reservation.complete()
        reservation.table.release(reservation)
        return reservation
    }

    fun cancel(reservationId: String): Reservation {
        val reservation = reservation(reservationId)
        reservation.cancel()
        reservation.table.release(reservation)
        return reservation
    }

    fun reservationsOf(customer: Customer): List<Reservation> {
        return reservations.values.filter { it.customer.id == customer.id }
    }

    private fun reservation(id: String): Reservation {
        val reservation = reservations[id]
        requireNotNull(reservation) { "Reservation $id not found" }
        return reservation
    }

    private fun intervalFrom(start: LocalDateTime): TimeInterval {
        val end = start.plus(slotDuration)
        val opening = start.toLocalDate().atTime(opensAt)
        val closing = start.toLocalDate().atTime(closesAt)
        require(!start.isBefore(opening) && !end.isAfter(closing)) { "$start is outside opening hours" }
        return TimeInterval(start, end)
    }
}
