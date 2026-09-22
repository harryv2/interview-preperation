package lld.restaurantreservation.entity

enum class ReservationStatus {
    CONFIRMED,
    SEATED,
    COMPLETED,
    CANCELLED
}

class Reservation(
    val id: String,
    val customer: Customer,
    val table: Table,
    val partySize: Int,
    val interval: TimeInterval
) {
    var status: ReservationStatus = ReservationStatus.CONFIRMED
        private set

    fun seat() {
        check(status == ReservationStatus.CONFIRMED) { "Reservation $id is $status" }
        status = ReservationStatus.SEATED
    }

    fun complete() {
        check(status == ReservationStatus.SEATED) { "Reservation $id is $status" }
        status = ReservationStatus.COMPLETED
    }

    fun cancel() {
        check(status == ReservationStatus.CONFIRMED) { "Reservation $id is $status" }
        status = ReservationStatus.CANCELLED
    }
}
