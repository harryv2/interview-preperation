package lld.eventbooking.entity


enum class SectionKind {
    SEATED,
    STANDING
}


class Seat(
    val id: String,
    val label: String
)


class Section(
    val id: String,
    val name: String,
    val kind: SectionKind,
    val seats: List<Seat>,
    val capacity: Int
) {

    companion object {

        fun seated(id: String, name: String, rows: List<String>, seatsPerRow: Int): Section {
            val seats = rows.flatMap { row ->
                (1..seatsPerRow).map { Seat("$id-$row$it", "$row$it") }
            }
            return Section(id, name, SectionKind.SEATED, seats, seats.size)
        }

        fun standing(id: String, name: String, capacity: Int): Section {
            return Section(id, name, SectionKind.STANDING, emptyList(), capacity)
        }
    }
}


class Venue(
    val id: String,
    val name: String,
    val city: String,
    val sections: List<Section>
) {

    private val sectionsById = sections.associateBy { it.id }

    fun section(sectionId: String): Section {
        return requireNotNull(sectionsById[sectionId]) { "Venue $name has no section $sectionId" }
    }

    override fun toString(): String {
        return "$name, $city"
    }
}
