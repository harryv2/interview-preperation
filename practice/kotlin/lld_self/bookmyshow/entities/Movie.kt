package lld_self.bookmyshow.entities

import kotlin.time.Duration
import kotlin.uuid.Uuid

data class Movie(
    val name: String,
    val description: String,
    val genre: String,
    val duration: Duration,
    val actors: List<String>,
) {
    val id = Uuid.random()
}