package lld.hotel.entity

import java.time.LocalDate


class Guest(val id: String, val name: String) {
    override fun toString(): String = name
}


enum class ReservationStatus {
    CONFIRMED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED
}


// A booking is against a room TYPE. The actual room is decided at check in, which is what lets the hotel
// shuffle who goes where right up to arrival.
class Reservation(
    val id: String,
    val hotelId: String,
    val guest: Guest,
    val type: RoomType,
    val stay: DateRange,
    val total: Money
) {

    var status: ReservationStatus = ReservationStatus.CONFIRMED
        private set

    var room: Room? = null
        private set

    // a cancelled booking stops consuming a room of its type, everything else still does
    fun holdsInventory(): Boolean {
        return status != ReservationStatus.CANCELLED
    }

    // a key is handed over on the arrival day or later, never before, and never once the stay is over. Late
    // arrival is normal, the room was held from the first night either way.
    fun checkIn(assigned: Room, today: LocalDate) {
        check(status == ReservationStatus.CONFIRMED) { "Reservation $id is $status" }
        require(!today.isBefore(stay.checkIn)) { "Reservation $id starts ${stay.checkIn}, too early to check in on $today" }
        require(today.isBefore(stay.checkOut)) { "Reservation $id ended ${stay.checkOut}, that is a no show" }
        require(assigned.type == type) { "Room ${assigned.number} is a ${assigned.type}, not a $type" }

        room = assigned
        status = ReservationStatus.CHECKED_IN
    }

    fun checkOut() {
        check(status == ReservationStatus.CHECKED_IN) { "Reservation $id is $status, nobody checked in" }
        status = ReservationStatus.CHECKED_OUT
    }

    fun cancel() {
        check(status == ReservationStatus.CONFIRMED) { "Reservation $id is $status, too late to cancel" }
        status = ReservationStatus.CANCELLED
    }

    override fun toString(): String {
        val where = room?.let { "room ${it.number}" } ?: "$type, room not assigned yet"
        return "$id ${guest.name} at $hotelId, $where $stay $total [$status]"
    }
}
