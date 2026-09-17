package lld_self.bookmyshow.entities

import kotlin.uuid.Uuid


enum class SeatType {
    REGULAR,
    PREMIUM,
    LUXURY
}

class Seat(
    val type: SeatType,
    val name: String,
    val basePrice: Money,
) {
    val id = Uuid.random()
}

class Screen(
    val name: String,
    val theaterId: Uuid,
    val seats: List<Seat>
) {
    val id = Uuid.random()
}