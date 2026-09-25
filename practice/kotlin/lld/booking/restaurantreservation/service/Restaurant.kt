package lld.booking.restaurantreservation.service

import lld.booking.restaurantreservation.entity.Customer
import lld.booking.restaurantreservation.entity.Reservation
import lld.booking.restaurantreservation.entity.Table
import lld.booking.restaurantreservation.entity.TimeInterval
import lld.booking.restaurantreservation.entity.WaitlistEntry
import lld.booking.restaurantreservation.strategy.TableSelectionStrategy
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Restaurant(
    val name: String,
    private val tables: List<Table>,
    private val strategy: TableSelectionStrategy,
    private val opensAt: LocalTime = LocalTime.of(11, 0),
    private val closesAt: LocalTime = LocalTime.of(23, 0),
    private val slotDuration: Duration = Duration.ofHours(2)
) {
    private val reservations = ConcurrentHashMap<String, Reservation>()
    private val waitlist = ConcurrentHashMap<String, WaitlistEntry>()
    private val waiting = ConcurrentLinkedQueue<WaitlistEntry>()
    private val waitlistLock = ReentrantLock()

    fun availableTables(partySize: Int, start: LocalDateTime): List<Table> {
        return strategy.candidates(tables, partySize, intervalFrom(start))
    }

    fun reserve(customer: Customer, partySize: Int, start: LocalDateTime): Reservation? {
        return reserve(customer, partySize, intervalFrom(start))
    }

    fun joinWaitlist(customer: Customer, partySize: Int, start: LocalDateTime): WaitlistEntry {
        val interval = intervalFrom(start)
        require(tables.any { it.fits(partySize) }) { "No table seats a party of $partySize" }

        val entry = WaitlistEntry(UUID.randomUUID().toString(), customer, partySize, interval)
        waitlist[entry.id] = entry
        waiting.add(entry)
        return entry
    }

    fun leaveWaitlist(entryId: String): WaitlistEntry {
        waitlistLock.withLock {
            val entry = waitlistEntry(entryId)
            entry.cancel()
            waiting.remove(entry)
            return entry
        }
    }

    fun waitlistOf(customer: Customer): List<WaitlistEntry> {
        return waitlist.values.filter { it.customer.id == customer.id }
    }

    fun waitingFor(start: LocalDateTime): List<WaitlistEntry> {
        val interval = intervalFrom(start)
        return waiting.filter { it.interval == interval }
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
        promoteWaiting(reservation.interval)
        return reservation
    }

    fun cancel(reservationId: String): Reservation {
        val reservation = reservation(reservationId)
        reservation.cancel()
        reservation.table.release(reservation)
        promoteWaiting(reservation.interval)
        return reservation
    }

    fun reservationsOf(customer: Customer): List<Reservation> {
        return reservations.values.filter { it.customer.id == customer.id }
    }

    private fun reserve(customer: Customer, partySize: Int, interval: TimeInterval): Reservation? {
        for (table in strategy.candidates(tables, partySize, interval)) {
            val reservation = Reservation(UUID.randomUUID().toString(), customer, table, partySize, interval)

            if (table.tryReserve(reservation)) {
                reservations[reservation.id] = reservation
                return reservation
            }
        }

        return null
    }

    private fun promoteWaiting(freed: TimeInterval) {
        waitlistLock.withLock {
            for (entry in waiting) {
                if (!entry.interval.overlaps(freed)) {
                    continue
                }

                val reservation = reserve(entry.customer, entry.partySize, entry.interval)

                if (reservation != null) {
                    entry.promote(reservation)
                    waiting.remove(entry)
                }
            }
        }
    }

    private fun reservation(id: String): Reservation {
        val reservation = reservations[id]
        requireNotNull(reservation) { "Reservation $id not found" }
        return reservation
    }

    private fun waitlistEntry(id: String): WaitlistEntry {
        val entry = waitlist[id]
        requireNotNull(entry) { "Waitlist entry $id not found" }
        return entry
    }

    private fun intervalFrom(start: LocalDateTime): TimeInterval {
        val end = start.plus(slotDuration)
        val opening = start.toLocalDate().atTime(opensAt)
        val closing = start.toLocalDate().atTime(closesAt)
        require(!start.isBefore(opening) && !end.isAfter(closing)) { "$start is outside opening hours" }
        return TimeInterval(start, end)
    }
}
