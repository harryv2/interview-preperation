package lld.fintech.splitwise.entity

import kotlin.math.absoluteValue

// =====================================================================
//  MONEY
//
//  Every amount is an integer count of paise. Never Double.
//  0.1 + 0.2 != 0.3 in binary floating point, so repeated addition
//  drifts and the "splits must add up to the total" rule breaks.
//  Long holds ~92 quadrillion paise, which is plenty.
//
//  `value class` means this compiles down to a plain `long` at runtime,
//  so the type safety costs no memory.
// =====================================================================

@JvmInline
value class Money(val paise: Long) : Comparable<Money> {

    operator fun plus(other: Money) = Money(paise + other.paise)
    operator fun minus(other: Money) = Money(paise - other.paise)
    operator fun unaryMinus() = Money(-paise)

    override fun compareTo(other: Money): Int = paise.compareTo(other.paise)

    override fun toString(): String {
        val sign = if (paise < 0) "-" else ""
        val abs = paise.absoluteValue
        return "$sign₹${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    companion object {
        val ZERO = Money(0)
        fun rupees(amount: Long) = Money(amount * 100)
        fun paise(amount: Long) = Money(amount)
    }
}
