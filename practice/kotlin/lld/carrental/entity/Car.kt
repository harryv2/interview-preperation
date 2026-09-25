package lld.carrental.entity

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


enum class CarType {
    HATCHBACK,
    SEDAN,
    SUV
}


class Car(
    val id: String,
    val model: String,
    val type: CarType
) {

    private val lock = ReentrantLock()
    private val booked = mutableListOf<TimeRange>()

    fun isFree(range: TimeRange): Boolean {
        lock.withLock {
            return booked.none { it.overlaps(range) }
        }
    }

    fun tryReserve(range: TimeRange): Boolean {
        lock.withLock {
            if (booked.any { it.overlaps(range) }) {
                return false
            }
            booked.add(range)
            return true
        }
    }

    fun release(range: TimeRange) {
        lock.withLock {
            booked.remove(range)
        }
    }

    override fun toString(): String {
        return "$model ($type, $id)"
    }
}
