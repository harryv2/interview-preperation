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
import lld_self.bookmyshow.repository.BookingRepository
import lld_self.bookmyshow.repository.ShowRepository
import lld_self.bookmyshow.repository.TheaterRepository
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid


class BookMyShowService(
    private val theaterRepo: TheaterRepository,
    private val showRepo: ShowRepository,
    private val bookingRepo: BookingRepository,
    private val lockTtl: Duration = 10.minutes,
    sweepInterval: Duration = 1.minutes,
) {

    fun createTheatre(name: String, city: String, location: Location, screens: List<String>): Theater {
        val theaterId = Uuid.random()

        val theater = Theater(
            name,
            theaterId,
            city,
            location,
            screens = screens.map { Screen(it, theaterId, buildSeats()) }
        )

        return theaterRepo.save(theater)
    }

    private fun buildSeats(): List<Seat> {
        val rowTypes = List(10) { SeatType.REGULAR } + List(10) { SeatType.PREMIUM } + List(2) { SeatType.LUXURY }

        return rowTypes.flatMapIndexed { rowIdx, type ->
            val row = 'A' + rowIdx
            (1..SEATS_PER_ROW).map { Seat(type, "$row$it", SEAT_PRICES.getValue(type)) }
        }
    }

    fun addShow(theaterId: Uuid, movie: Movie, screenId: Uuid, startTime: Instant): Show {
        val theater = theaterRepo.findById(theaterId)
        require(theater != null) { "Theater not found" }

        val screen = theater.screenMap[screenId]
        require(screen != null) { "Screen not in theater ${theater.name}" }

        val show = Show(movie, screen, startTime)

        synchronized(theater) {
            val clash = showRepo.findByScreen(screenId).firstOrNull { it.overlaps(show) }
            require(clash == null) { "Show overlaps with existing show: $clash" }

            return showRepo.save(show)
        }
    }

    fun getMovies(city: String): List<Movie> {
        val now = Clock.System.now()

        return theaterRepo.findByCity(city)
            .flatMap { showRepo.findByTheater(it.id) }
            .filter { it.startTime > now }
            .map { it.movie }
            .distinct()
    }

    fun getShows(city: String, movie: Movie): Map<Theater, List<Show>> {
        val now = Clock.System.now()

        return theaterRepo.findByCity(city)
            .associateWith { theater ->
                showRepo.findByTheater(theater.id)
                    .filter { it.movie == movie && it.startTime > now }
                    .sortedBy { it.startTime }
            }
            .filterValues { it.isNotEmpty() }
    }

    fun getAvailableSeats(showId: Uuid): List<ShowSeat> {
        val show = showRepo.findById(showId)
        require(show != null) { "Show not found" }

        return show.getAvailableSeats()
    }

    fun createBooking(user: User, showId: Uuid, seatIds: List<Uuid>): Booking {
        require(seatIds.size <= MAX_SEATS_PER_BOOKING) { "At most $MAX_SEATS_PER_BOOKING seats per booking" }

        val show = showRepo.findById(showId)
        require(show != null) { "Show not found" }

        val bookingId = Uuid.random()

        val showSeats = show.reserveSeats(seatIds, bookingId, lockTtl)

        val booking = Booking(bookingId, user, show, showSeats, expiresAt = Clock.System.now() + lockTtl)
        return bookingRepo.save(booking)
    }

    fun makePayment(bookingId: Uuid, paymentMethod: PaymentMethod): Booking {
        val booking = bookingRepo.findById(bookingId)
        require(booking != null) { "Booking not found" }

        if (booking.status == BookingStatus.CREATED && booking.isExpired()) expire(booking)
        require(booking.status == BookingStatus.CREATED) { "Booking is ${booking.status}" }

        try {
            val payment = paymentMethod.pay(booking.totalAmount)
            booking.show.confirmSeats(booking)
            booking.markPaid(payment)
        } catch (e: PaymentFailedException) {
            booking.markFailed()
            booking.show.releaseSeats(booking)
        }
        return bookingRepo.save(booking)
    }

    private val sweeper = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "booking-sweeper").apply { isDaemon = true }
    }

    init {
        sweeper.scheduleAtFixedRate(
            { sweepExpiredBookings() },
            sweepInterval.inWholeMilliseconds, sweepInterval.inWholeMilliseconds, TimeUnit.MILLISECONDS
        )
    }

    private fun sweepExpiredBookings() {
        bookingRepo.findByStatus(BookingStatus.CREATED).forEach {
            if (it.isExpired()) {
                runCatching { expire(it) }
            }
        }
    }

    private fun expire(booking: Booking) {
        booking.markExpired()
        booking.show.releaseSeats(booking)
        bookingRepo.save(booking)
    }

    companion object {
        private const val SEATS_PER_ROW = 10
        private const val MAX_SEATS_PER_BOOKING = 10
        private val SEAT_PRICES = mapOf(
            SeatType.REGULAR to Money.rupees(200),
            SeatType.PREMIUM to Money.rupees(300),
            SeatType.LUXURY to Money.rupees(400),
        )
    }
}
