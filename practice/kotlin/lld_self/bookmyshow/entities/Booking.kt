package lld_self.bookmyshow.entities

import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
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
) {
    var status = BookingStatus.CREATED
        private set
    val totalAmount = Money.paise(showSeats.sumOf { it.price.paise })
    val createdAt = Clock.System.now()
    val expiresAt = createdAt + 10.minutes

    var payment: Payment? = null
        private set


    fun markExpired(){
        require(Clock.System.now() > expiresAt) {"No expired yet"}
        check(status == BookingStatus.CREATED) {"State not created"}

        status = BookingStatus.EXPIRED
    }

    fun isExpired(): Boolean {
        return status == BookingStatus.EXPIRED || (status == BookingStatus.CREATED && Clock.System.now() > expiresAt)
    }

    fun markPaid(payment: Payment) {
        check(status == BookingStatus.CREATED) {"State not created"}

        status = BookingStatus.SUCCESS
        this.payment = payment
    }

    fun markFailed() {
        check(status == BookingStatus.CREATED) {"State not created"}
        status = BookingStatus.FAILED
    }
}