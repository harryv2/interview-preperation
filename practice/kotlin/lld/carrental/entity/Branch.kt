package lld.carrental.entity


class Branch(
    val id: String,
    val name: String,
    cars: List<Car>
) {

    private val carsByType = cars.groupBy { it.type }

    fun cars(type: CarType): List<Car> {
        return carsByType[type].orEmpty()
    }

    fun available(type: CarType, range: TimeRange): List<Car> {
        return cars(type).filter { it.isFree(range) }
    }

    override fun toString(): String {
        return "$name ($id)"
    }
}
