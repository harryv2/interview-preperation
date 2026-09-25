package lld.commerce.restaurantordering.entity


enum class Station {
    KITCHEN,
    BAR
}

enum class Course {
    STARTER,
    MAIN,
    DESSERT,
    DRINK
}

class MenuItem(
    val id: String,
    val name: String,
    val course: Course,
    val station: Station,
    price: Money
) {

    var price: Money = price
        private set

    var available: Boolean = true
        private set

    fun reprice(to: Money) {
        price = to
    }

    fun setAvailable(value: Boolean) {
        available = value
    }

    override fun toString(): String {
        return "$name $price"
    }
}
