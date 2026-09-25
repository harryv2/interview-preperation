package lld.orderbook.entity

import kotlin.math.abs


data class Price(
    val ticks: Long
) : Comparable<Price> {

    override fun compareTo(other: Price): Int {
        return ticks.compareTo(other.ticks)
    }

    override fun toString(): String {
        val sign = if (ticks < 0) "-" else ""
        val absolute = abs(ticks)
        return "$sign${absolute / 100}.${"%02d".format(absolute % 100)}"
    }

    companion object {
        fun of(whole: Long, cents: Long = 0): Price {
            return Price(whole * 100 + cents)
        }
    }
}
