package lld_self.bookmyshow

import lld_self.bookmyshow.entities.Location
import lld_self.bookmyshow.entities.Movie
import lld_self.bookmyshow.entities.UPIPayment
import lld_self.bookmyshow.entities.User
import lld_self.bookmyshow.entities.WalletPayment
import lld_self.bookmyshow.repository.InMemoryBookingRepository

import lld_self.bookmyshow.repository.InMemoryShowRepository
import lld_self.bookmyshow.repository.InMemoryTheaterRepository
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


fun main() {

    val app = BookMyShowService(
        InMemoryTheaterRepository(),
        InMemoryShowRepository(),
        InMemoryBookingRepository()
    )

    val pvr = app.createTheatre("PVR Orion", "Bengaluru", Location(12.99, 77.55), listOf("Audi 1", "Audi 2"))
    val inox = app.createTheatre("INOX Garuda", "Bengaluru", Location(12.97, 77.61), listOf("Screen A"))

    val interstellar = Movie("Interstellar", "Space epic", "Sci-Fi", 169.minutes, listOf("Matthew McConaughey"))
    val inception = Movie("Inception", "Dream heist", "Thriller", 148.minutes, listOf("Leonardo DiCaprio"))

    val now = Clock.System.now()
    app.addShow(pvr.id, interstellar, pvr.screens[0].id, now + 2.hours)
    app.addShow(pvr.id, inception, pvr.screens[1].id, now + 3.hours)
    app.addShow(inox.id, interstellar, inox.screens[0].id, now + 4.hours)

    println(app.getMovies("Bengaluru"))

    val shows = app.getShows("Bengaluru", interstellar)
    println(shows)

    val show = shows[pvr]!!.first()
    println("Free seats: ${app.getAvailableSeats(show.id).size}")

    val alice = User("alice")
    val bob = User("bob")

    val seats = app.getAvailableSeats(show.id).take(3)
    val aliceBooking = app.createBooking(alice, show.id, seats.map { it.seat.id })
    println(aliceBooking)

    try {
        app.createBooking(bob, show.id, seats.map { it.seat.id })
    } catch (e: IllegalArgumentException) {
        println("Bob rejected: ${e.message}")
    }

    app.makePayment(aliceBooking.id, UPIPayment())
    println(aliceBooking)
    println("Free seats: ${app.getAvailableSeats(show.id).size}")

    val bobSeats = app.getAvailableSeats(show.id).take(2)
    val bobBooking = app.createBooking(bob, show.id, bobSeats.map { it.seat.id })
    app.makePayment(bobBooking.id, WalletPayment())
    println(bobBooking)
    println("Free seats: ${app.getAvailableSeats(show.id).size}")

    val hotSeats = app.getAvailableSeats(show.id).take(2).map { it.seat.id }
    val start = CountDownLatch(1)
    var winners = 0

    val threads = (1..5).map { i ->
        thread {
            start.await()
            try {
                app.createBooking(User("user-$i"), show.id, hotSeats)
                synchronized(app) { winners++ }
                println("user-$i got the seats")
            } catch (e: IllegalArgumentException) {
                println("user-$i rejected: ${e.message}")
            }
        }
    }
    start.countDown()
    threads.forEach { it.join() }
    println("Winners: $winners")
}
