package lld_self.bookmyshow

import lld_self.bookmyshow.entities.Booking
import lld_self.bookmyshow.entities.BookingStatus
import lld_self.bookmyshow.entities.Location
import lld_self.bookmyshow.entities.Money
import lld_self.bookmyshow.entities.Movie
import lld_self.bookmyshow.entities.PaymentFailedException
import lld_self.bookmyshow.entities.PaymentMethod
import lld_self.bookmyshow.entities.Screen
import lld_self.bookmyshow.entities.Seat
import lld_self.bookmyshow.entities.SeatType
import lld_self.bookmyshow.entities.Show
import lld_self.bookmyshow.entities.ShowSeat
import lld_self.bookmyshow.entities.Theater
import lld_self.bookmyshow.entities.User
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

var multiplierMap = hashMapOf(
    SeatType.REGULAR to 1.0,
    SeatType.REGULAR to 1.5,
    SeatType.REGULAR to 2.0,
)

class BookMyShowService {

    val cityWiseTheatres = hashMapOf<String, MutableList<Theater>>()
    private val bookings = ConcurrentHashMap<Uuid, Booking>()

    fun createTheatre(name: String, city: String, location: Location, screens: List<String>): Theater {
        val seatTypes = List(10) { SeatType.REGULAR } + List(10) { SeatType.PREMIUM } + List(2) { SeatType.LUXURY }

        val theaterId = Uuid.random()

        val theater = Theater(
            name,
            theaterId,
            city,
            location,
            screens = screens.map {
                var row = 'A'
                val seats = seatTypes.map { t ->
                    var seatInRow = mutableListOf<Seat>()
                    var i = 1
                    repeat(10) {
                        println("${row}$i")
                        seatInRow.add(Seat(t, "${row}$i", Money.rupees(200 * multiplierMap[t]!!.toLong())))
                    }
                    seatInRow
                }

                Screen(it, theaterId, seats.flatten())
            }
        )

        cityWiseTheatres.putIfAbsent(city, mutableListOf())
        cityWiseTheatres[city]!!.add(theater)
        return theater
    }

    fun getShows(city: String, movie: Movie): Map<Theater, List<Show>> {
        require(cityWiseTheatres.contains(city)) { "No theatres in $city" }

        return cityWiseTheatres[city]!!.associateWith { it.getShows(movie) }
    }


    fun createBooking(user: User, show: Show, seats: List<Uuid>): Booking {
        var bookingId = Uuid.random()

        val showSeats = show.reserveSeats(seats, bookingId, 10.minutes)

        var booking = Booking(
            bookingId,
            user,
            show,
            showSeats
        )

        return booking
    }


    fun makePayment(bookingId: Uuid, paymentMethod: PaymentMethod) {
        val booking = bookings[bookingId]
        require(booking != null) { "Booking not found" }
        require(booking.status == BookingStatus.CREATED) { "Booking in created" }

        try {
            val payment = paymentMethod.pay(booking.totalAmount)
            booking.show.confirmSeats(booking)
            booking.markPaid(payment)
        } catch (e: PaymentFailedException) {
            booking.markFailed()
            booking.show.releaseSeats(booking)
        }
    }


    private val sweeper = Executors.newSingleThreadScheduledExecutor()

    init {
        sweeper.scheduleAtFixedRate({
            bookings.values.filter {
                it.status == BookingStatus.CREATED && it.isExpired()
            }.forEach { expire(it) }
        }, 1, 1, TimeUnit.MINUTES)
    }

    private fun expire(b: Booking) {
        b.markExpired();
        b.show.releaseSeats(b)
    }

}