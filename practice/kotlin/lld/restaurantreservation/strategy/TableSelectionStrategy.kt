package lld.restaurantreservation.strategy

import lld.restaurantreservation.entity.Table
import lld.restaurantreservation.entity.TimeInterval

interface TableSelectionStrategy {
    fun candidates(tables: List<Table>, partySize: Int, interval: TimeInterval): List<Table>
}
