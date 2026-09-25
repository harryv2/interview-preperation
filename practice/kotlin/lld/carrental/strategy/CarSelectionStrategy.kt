package lld.carrental.strategy

import lld.carrental.entity.Car
import lld.carrental.entity.TimeRange


interface CarSelectionStrategy {
    fun pick(cars: List<Car>, range: TimeRange): Car?
}


class FirstAvailableCar : CarSelectionStrategy {
    override fun pick(cars: List<Car>, range: TimeRange): Car? {
        return cars.firstOrNull { it.isFree(range) }
    }
}
