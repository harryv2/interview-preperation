package lld_self.splitwise.entity

import kotlin.math.absoluteValue

@JvmInline
value class Money(
    val paise:  Long
): Comparable<Money> {

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


data class Transfer(
    val from: User,
    val to: User,
    val amount: Money
) {
    override fun toString(): String {
        return "$from - to - $to give $amount"
    }
}
