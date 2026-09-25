package lld.booking.restaurantreservation.entity

enum class WaitlistStatus {
    WAITING,
    PROMOTED,
    CANCELLED
}

class WaitlistEntry(
    val id: String,
    val customer: Customer,
    val partySize: Int,
    val interval: TimeInterval
) {
    var status: WaitlistStatus = WaitlistStatus.WAITING
        private set

    var reservation: Reservation? = null
        private set

    fun promote(reservation: Reservation) {
        check(status == WaitlistStatus.WAITING) { "Waitlist entry $id is $status" }
        this.reservation = reservation
        status = WaitlistStatus.PROMOTED
    }

    fun cancel() {
        check(status == WaitlistStatus.WAITING) { "Waitlist entry $id is $status" }
        status = WaitlistStatus.CANCELLED
    }
}
