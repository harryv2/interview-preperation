package lld.booking.hotel.service

import lld.booking.hotel.entity.DateRange
import lld.booking.hotel.entity.Guest
import lld.booking.hotel.entity.Hotel
import lld.booking.hotel.entity.Money
import lld.booking.hotel.entity.Reservation
import lld.booking.hotel.entity.RoomType
import java.util.concurrent.ConcurrentHashMap


class Offer(
    val hotel: Hotel,
    val type: RoomType,
    val stay: DateRange,
    val roomsLeft: Int,
    val total: Money
) {

    override fun toString(): String {
        return "${hotel.name} $type $total, $roomsLeft left"
    }
}


// The platform layer above the properties. It owns routing and nothing else: which hotels exist, and which
// one a reservation id came from. Inventory, pricing and the stay lifecycle stay inside the Hotel that sold
// the room, so each property keeps its own lock and a busy hotel never blocks a quiet one.
class BookingService {

    private val hotels = ConcurrentHashMap<String, Hotel>()

    // an index, not the fact. Reservation.hotelId is where a stay records the property it belongs to, and
    // this only exists because a desk call arrives as a bare id with no reservation in hand yet.
    private val hotelByReservation = ConcurrentHashMap<String, Hotel>()

    fun register(hotel: Hotel) {
        val clash = hotels.putIfAbsent(hotel.id, hotel)
        require(clash == null) { "Hotel ${hotel.id} is already registered as ${clash?.name}" }
    }

    fun hotelsIn(city: String): List<Hotel> {
        return hotels.values.filter { it.city.equals(city, ignoreCase = true) }.sortedBy { it.name }
    }

    // A fan out of the same question every property already answers, cheapest first. An offer is a snapshot
    // and nothing is held by it, so book() is what actually decides, under that hotel's lock.
    fun search(city: String, type: RoomType, stay: DateRange): List<Offer> {
        return hotelsIn(city)
            .map { Offer(it, type, stay, it.freeRooms(type, stay), it.quote(type, stay)) }
            .filter { it.roomsLeft > 0 }
            .sortedBy { it.total }
    }

    fun book(guest: Guest, hotelId: String, type: RoomType, stay: DateRange): Reservation {
        val hotel = hotel(hotelId)
        val reservation = hotel.book(guest, type, stay)

        hotelByReservation[reservation.id] = hotel
        return reservation
    }

    fun checkIn(reservationId: String): Reservation {
        return seller(reservationId).checkIn(reservationId)
    }

    fun checkOut(reservationId: String): Reservation {
        return seller(reservationId).checkOut(reservationId)
    }

    fun cancel(reservationId: String): Reservation {
        return seller(reservationId).cancel(reservationId)
    }

    fun reservationsOf(guest: Guest): List<Reservation> {
        return hotels.values.flatMap { it.reservationsOf(guest) }.sortedBy { it.stay.checkIn }
    }

    private fun hotel(hotelId: String): Hotel {
        return requireNotNull(hotels[hotelId]) { "No hotel $hotelId" }
    }

    private fun seller(reservationId: String): Hotel {
        return requireNotNull(hotelByReservation[reservationId]) { "No reservation $reservationId" }
    }
}
