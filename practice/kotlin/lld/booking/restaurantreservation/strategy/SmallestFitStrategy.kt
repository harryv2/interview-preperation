package lld.booking.restaurantreservation.strategy

import lld.booking.restaurantreservation.entity.Table
import lld.booking.restaurantreservation.entity.TimeInterval

class SmallestFitStrategy : TableSelectionStrategy {
    override fun candidates(tables: List<Table>, partySize: Int, interval: TimeInterval): List<Table> {
        return tables
            .filter { it.fits(partySize) && it.isAvailable(interval) }
            .sortedBy { it.seats }
    }
}
