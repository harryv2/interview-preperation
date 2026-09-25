package lld.booking.carrental.service

import lld.booking.carrental.entity.Branch
import lld.booking.carrental.entity.Car
import lld.booking.carrental.entity.CarType
import lld.booking.carrental.entity.Money
import lld.booking.carrental.entity.Reservation
import lld.booking.carrental.entity.ReservationStatus
import lld.booking.carrental.entity.TimeRange
import lld.booking.carrental.strategy.CarSelectionStrategy
import lld.booking.carrental.strategy.PricingStrategy
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap


class RentalService(
    branches: List<Branch>,
    private val pricingStrategy: PricingStrategy,
    private val selectionStrategy: CarSelectionStrategy
) {

    private val branchesById = branches.associateBy { it.id }
    private val reservations = ConcurrentHashMap<String, Reservation>()

    fun search(branchId: String, type: CarType, range: TimeRange): List<Car> {
        return branch(branchId).available(type, range)
    }

    fun reserve(customerId: String, branchId: String, type: CarType, range: TimeRange): Reservation {
        val candidates = branch(branchId).cars(type)
        require(candidates.isNotEmpty()) { "Branch $branchId has no $type" }

        val car = claim(candidates, range)
        requireNotNull(car) { "No $type free at $branchId for that range" }

        val reservation = Reservation(
            id = UUID.randomUUID().toString().take(8),
            customerId = customerId,
            car = car,
            range = range,
            quote = pricingStrategy.quote(type, range)
        )
        reservations[reservation.id] = reservation
        return reservation
    }

    fun cancel(reservationId: String): Reservation {
        val reservation = reservation(reservationId)
        reservation.moveTo(ReservationStatus.CANCELLED)
        reservation.car.release(reservation.range)
        return reservation
    }

    fun pickUp(reservationId: String): Reservation {
        val reservation = reservation(reservationId)
        reservation.moveTo(ReservationStatus.PICKED_UP)
        return reservation
    }

    fun dropOff(reservationId: String, returnedAt: LocalDateTime): Reservation {
        val reservation = reservation(reservationId)
        reservation.moveTo(ReservationStatus.RETURNED)
        reservation.settle(chargeFor(reservation, returnedAt))
        reservation.car.release(reservation.range)
        return reservation
    }

    fun reservationsOf(customerId: String): List<Reservation> {
        return reservations.values.filter { it.customerId == customerId }
    }

    private fun claim(candidates: List<Car>, range: TimeRange): Car? {
        while (true) {
            val car = selectionStrategy.pick(candidates, range) ?: return null
            if (car.tryReserve(range)) {
                return car
            }
        }
    }

    private fun chargeFor(reservation: Reservation, returnedAt: LocalDateTime): Money {
        if (returnedAt <= reservation.range.end) {
            return reservation.quote
        }

        val hoursLate = Duration.between(reservation.range.end, returnedAt).toHours()
        val daysLate = (hoursLate + 23) / 24
        return reservation.quote + pricingStrategy.lateFeePerDay(reservation.car.type) * daysLate
    }

    private fun branch(branchId: String): Branch {
        return requireNotNull(branchesById[branchId]) { "No branch $branchId" }
    }

    private fun reservation(reservationId: String): Reservation {
        val reservation = reservations[reservationId]
        requireNotNull(reservation) { "No reservation $reservationId" }
        return reservation
    }
}
