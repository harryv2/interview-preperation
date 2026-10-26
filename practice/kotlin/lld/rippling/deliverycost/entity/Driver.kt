package lld.rippling.deliverycost.entity

import java.math.BigDecimal

data class Driver(
    val id: String,
    val hourlyRate: BigDecimal,
)
