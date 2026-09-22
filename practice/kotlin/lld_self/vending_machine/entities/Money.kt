package lld_self.vending_machine.entities

import kotlin.math.abs


data class Money(
    val paise: Long
) : Comparable<Money> {

    operator fun plus(other: Money) = Money(paise + other.paise)
    operator fun minus(other: Money) = Money(paise - other.paise)
    operator fun unaryMinus() = Money(-paise)

    override fun compareTo(other: Money): Int {
        return paise.compareTo(other.paise)
    }

    override fun toString(): String {
        val sign = if (paise < 0) "-" else ""
        val absolute = abs(paise)
        return "Rs $sign${absolute / 100}.${"%02d".format(absolute % 100)}"
    }

    companion object {
        val ZERO = Money(0)

        fun paise(paise: Long): Money {
            return Money(paise)
        }

        fun rupees(rupee: Long): Money {
            return Money(rupee * 100)
        }
    }
}
