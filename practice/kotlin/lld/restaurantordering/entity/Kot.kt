package lld.restaurantordering.entity

import java.time.Instant


class Kot(
    val id: String,
    val orderId: String,
    val tableNumber: String,
    val station: Station,
    val lines: List<OrderLine>,
    val punchedAt: Instant
) {

    fun isClosed(): Boolean {
        return lines.none { it.status == LineStatus.PLACED || it.status == LineStatus.PREPARING }
    }

    override fun toString(): String {
        return "KOT $id $station table $tableNumber -> ${lines.joinToString()}"
    }
}
