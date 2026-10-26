package lld.rippling.deliverycost.entity

import java.math.BigDecimal

data class Delivery(
    val driverId: String,
    val startSec: Long,
    val endSec: Long,
    val cost: BigDecimal,
)
