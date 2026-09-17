package lld_self.bookmyshow.entities

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid


enum class BookingStatus {
    CREATED,
    SUCCESS,
    FAILED,
    EXPIRED
}

class Booking(
    val id: Uuid,
    val user: User,
    val show: Show,
    val showSeats: List<ShowSeat>,
    val expiresAt: Instant,
) {
    var status = BookingStatus.CREATED
        private set
    val totalAmount = Money.paise(showSeats.sumOf { it.price.paise })
    val createdAt = Clock.System.now()

    var payment: Payment? = null
        private set

    fun isExpired(): Boolean {
        if (status == BookingStatus.EXPIRED) return true
        if (status == BookingStatus.CREATED && Clock.System.now() > expiresAt) return true
        return false
    }

    @Synchronized
    fun markExpired() {
        require(Clock.System.now() > expiresAt) { "Booking $id has not expired yet" }
        check(status == BookingStatus.CREATED) { "Booking $id is $status, cannot expire" }
        status = BookingStatus.EXPIRED
    }

    @Synchronized
    fun markPaid(payment: Payment) {
        check(status == BookingStatus.CREATED) { "Booking $id is $status, cannot mark paid" }
        status = BookingStatus.SUCCESS
        this.payment = payment
    }

    @Synchronized
    fun markFailed() {
        check(status == BookingStatus.CREATED) { "Booking $id is $status, cannot mark failed" }
        status = BookingStatus.FAILED
    }

    override fun toString() =
        "Booking(${user.name}, seats=${showSeats.map { it.seat.name }}, amount=$totalAmount, status=$status)"
}
