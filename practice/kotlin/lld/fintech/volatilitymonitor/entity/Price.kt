package lld.fintech.volatilitymonitor.entity

import kotlin.math.absoluteValue


// Prices are exact paise, never a Double. A monitor whose whole job is comparing small differences is the
// last place to accept a representation where 0.1 + 0.2 is not 0.3.
class Price(val paise: Long) : Comparable<Price> {

    operator fun minus(other: Price) = Price(paise - other.paise)

    fun abs() = Price(paise.absoluteValue)

    override fun compareTo(other: Price): Int = paise.compareTo(other.paise)

    override fun toString(): String {
        val abs = paise.absoluteValue
        val sign = if (paise < 0) "-" else ""
        return "$sign₹${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    companion object {
        fun paise(amount: Long) = Price(amount)
        fun rupees(amount: Double) = Price(Math.round(amount * 100))
    }
}


class Snapshot(
    val symbol: String,
    val trades: Int,
    val last: Price,
    val high: Price,
    val low: Price,
    val maxTickMove: Price
) {

    val range = high - low

    override fun toString(): String {
        return "$symbol last=$last high=$high low=$low range=$range maxTick=$maxTickMove over $trades trades"
    }
}
