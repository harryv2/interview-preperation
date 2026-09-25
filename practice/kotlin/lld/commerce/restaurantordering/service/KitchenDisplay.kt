package lld.commerce.restaurantordering.service

import lld.commerce.restaurantordering.entity.Kot
import lld.commerce.restaurantordering.entity.Station


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
