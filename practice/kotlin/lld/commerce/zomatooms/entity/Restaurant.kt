package lld.commerce.zomatooms.entity


data class MenuItem(
    val id: String,
    val name: String,
    val price: Money
)


class Restaurant(
    val id: String,
    val name: String,
    menu: List<MenuItem>,
    var isOpen: Boolean = true
) {

    private val menuById = menu.associateBy { it.id }

    fun find(itemId: String): MenuItem? {
        return menuById[itemId]
    }

    fun menu(): List<MenuItem> {
        return menuById.values.toList()
    }
}
