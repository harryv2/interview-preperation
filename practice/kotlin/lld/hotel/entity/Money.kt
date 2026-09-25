package lld.hotel.entity

import kotlin.math.absoluteValue

class Money(val paise: Long) : Comparable<Money> {

    operator fun plus(other: Money) = Money(paise + other.paise)
    operator fun times(count: Int) = Money(paise * count)

    fun percent(rate: Int) = Money(paise * rate / 100)

    override fun compareTo(other: Money): Int = paise.compareTo(other.paise)

    override fun toString(): String {
        val abs = paise.absoluteValue
        return "₹${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    companion object {
        val ZERO = Money(0)
        fun rupees(amount: Long) = Money(amount * 100)
    }
}
