package lld.fintech.splitwise.strategy

import lld.fintech.splitwise.entity.Money
import lld.fintech.splitwise.entity.User

/** Caller states exactly what each person owes. No remainder is possible. */
class ExactSplit(private val amounts: Map<User, Money>) : SplitStrategy {

    override val participants: Set<User> = amounts.keys

    init {
        require(amounts.isNotEmpty()) { "Exact split needs at least one participant" }
    }

    override fun computeOwedBy(total: Money, expenseId: String): Map<User, Money> {
        val sum = amounts.values.sumOf { it.paise }
        require(sum == total.paise) {
            "Exact amounts add up to ${Money(sum)} but the total is $total"
        }
        return amounts
    }
}
