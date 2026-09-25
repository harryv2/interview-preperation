package lld.commerce.restaurantordering.entity


class Menu(items: List<MenuItem>) {

    private val itemsById = items.associateBy { it.id }

    fun item(id: String): MenuItem {
        return requireNotNull(itemsById[id]) { "No menu item $id" }
    }

    fun available(): List<MenuItem> {
        return itemsById.values.filter { it.available }
    }

    fun byCourse(course: Course): List<MenuItem> {
        return available().filter { it.course == course }
    }

    fun setAvailable(id: String, value: Boolean) {
        item(id).setAvailable(value)
    }

    fun reprice(id: String, to: Money) {
        item(id).reprice(to)
    }
}
