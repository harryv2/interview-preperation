package lld.booking.restaurantreservation.strategy

import lld.booking.restaurantreservation.entity.Table
import lld.booking.restaurantreservation.entity.TimeInterval

interface TableSelectionStrategy {
    fun candidates(tables: List<Table>, partySize: Int, interval: TimeInterval): List<Table>
}
