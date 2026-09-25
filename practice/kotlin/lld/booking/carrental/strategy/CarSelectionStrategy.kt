package lld.booking.carrental.strategy

import lld.booking.carrental.entity.Car
import lld.booking.carrental.entity.TimeRange


interface CarSelectionStrategy {
    fun pick(cars: List<Car>, range: TimeRange): Car?
}


class FirstAvailableCar : CarSelectionStrategy {
    override fun pick(cars: List<Car>, range: TimeRange): Car? {
        return cars.firstOrNull { it.isFree(range) }
    }
}
