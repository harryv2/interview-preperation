package lld.eventbooking.entity


enum class EventCategory {
    CONCERT,
    CONFERENCE,
    SPORTS,
    THEATRE
}


class TicketTier(
    val id: String,
    val name: String,
    val sectionId: String,
    val basePrice: Money
) {
    override fun toString(): String {
        return "$name $basePrice"
    }
}


class Event(
    val id: String,
    val title: String,
    val category: EventCategory,
    val maxTicketsPerUser: Int
) {
    override fun toString(): String {
        return "$title ($category)"
    }
}
