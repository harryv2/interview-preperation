package lld_self.bookmyshow.entities

import kotlin.time.Instant
import kotlin.uuid.Uuid


data class Location(val lat: Double, val long: Double)


class Theater(
    val name: String,
    val id: Uuid,
    val city: String,
    val location: Location,
    val screens: List<Screen>
) {
    val screenMap = screens.associateBy { it.id }


    fun getShows(movie: Movie): List<Show> {
        TODO("implement")
    }

    fun addShows(movie: Movie, startTime: Instant, screenId: String) {
        require("")
        TODO("implement")
    }
}