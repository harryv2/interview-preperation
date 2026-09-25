package lld.restaurantordering.service

import lld.restaurantordering.entity.Kot
import lld.restaurantordering.entity.Station


class KitchenDisplay(
    val station: Station,
    private val service: RestaurantService
) {

    fun queue(): List<Kot> {
        return service.pendingKots(station)
    }

    fun accept(kotId: String) {
        service.startPreparing(kotId)
    }

    fun ready(kotId: String) {
        service.markReady(kotId)
    }
}
