package lld.carrental.entity

import java.time.LocalDateTime


enum class ReservationStatus {
    PENDING,
    PICKED_UP,
    RETURNED,
    CANCELLED;

    fun canMoveTo(next: ReservationStatus): Boolean {
        return next in ALLOWED[this].orEmpty()
    }

    companion object {
        private val ALLOWED = mapOf(
            PENDING to setOf(PICKED_UP, CANCELLED),
            PICKED_UP to setOf(RETURNED)
        )
    }
}


class Reservation(
    val id: String,
    val customerId: String,
    val car: Car,
    val range: TimeRange,
    val quote: Money
) {

    var status: ReservationStatus = ReservationStatus.PENDING
        private set

    var finalCharge: Money? = null
        private set

    fun moveTo(next: ReservationStatus) {
        require(status.canMoveTo(next)) { "Reservation $id can not move from $status to $next" }
        status = next
    }

    fun settle(amount: Money) {
        finalCharge = amount
    }

    override fun toString(): String {
        val charge = finalCharge ?: quote
        return "Reservation $id [$status] $car ${range.start} to ${range.end} -> $charge"
    }
}


data class Customer(
    val id: String,
    val name: String
)
