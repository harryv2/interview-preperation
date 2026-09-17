package lld_self.bookmyshow.entities

import kotlin.uuid.Uuid

data class User (
    val name: String
) {
    val id = Uuid.random()
}