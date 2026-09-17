package lld_self.bookmyshow.entities

import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
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
    private val screenMap = screens.associateBy { it.id }
    private val shows = mutableListOf<Show>()

    fun getShows(movie: Movie): List<Show> {
        val now = Clock.System.now()
        return getUpcomingShows(now).filter { it.movie == movie }
    }

    @Synchronized
    fun getUpcomingShows(now: Instant = Clock.System.now()): List<Show> {
        return shows.filter { it.startTime > now }.sortedBy { it.startTime }
    }

    @Synchronized
    fun addShow(movie: Movie, screenId: Uuid, startTime: Instant): Show {
        val screen = screenMap[screenId] ?: throw IllegalArgumentException("Screen $screenId not in theater $name")

        val show = Show(movie, screen, startTime)
        val clash = shows.firstOrNull { it.screen.id == screenId && it.overlaps(show) }
        require(clash == null) { "Show overlaps with existing show: $clash" }

        shows.add(show)
        return show
    }

    private fun Show.overlaps(other: Show): Boolean {
        return startTime < other.endTime + CLEANUP_BUFFER && other.startTime < endTime + CLEANUP_BUFFER
    }

    override fun toString() = "$name ($city)"

    companion object {
        private val CLEANUP_BUFFER = 30.minutes
    }
}
