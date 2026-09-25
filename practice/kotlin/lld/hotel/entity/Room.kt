package lld.hotel.entity


enum class RoomType(val baseRate: Money, val sleeps: Int) {
    SINGLE(Money.rupees(3000), 1),
    DOUBLE(Money.rupees(5000), 2),
    SUITE(Money.rupees(12000), 4)
}


class Room(
    val number: String,
    val type: RoomType
) {

    private val stays = mutableMapOf<String, DateRange>()

    fun isFree(stay: DateRange): Boolean {
        return stays.values.none { it.overlaps(stay) }
    }

    fun hold(reservationId: String, stay: DateRange) {
        require(isFree(stay)) { "Room $number is already taken for $stay" }
        stays[reservationId] = stay
    }

    fun release(reservationId: String) {
        stays.remove(reservationId)
    }

    fun stays(): List<DateRange> {
        return stays.values.sortedBy { it.checkIn }
    }

    override fun toString(): String {
        return "$number ($type)"
    }
}
