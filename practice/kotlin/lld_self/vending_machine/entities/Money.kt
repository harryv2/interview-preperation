package lld_self.vending_machine.entities


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
        return "Rs ${paise / 100}.${paise % 100}"
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