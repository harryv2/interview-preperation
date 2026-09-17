package lld_self.splitwise.strategies.split

import lld_self.splitwise.entity.Money
import lld_self.splitwise.entity.User
import kotlin.uuid.Uuid


class PercentageSplitStrategy(
    val splitMap: Map<User, Int>
) : SplitStrategy {
    override val participants: Set<User>
        get() = splitMap.keys

    override fun getOwned(
        totalAmount: Money,
        expenseId: Uuid
    ): Map<User, Money> {

        val totalPercentage = splitMap.values.sum()
        require(totalPercentage == 100) { "Percentage total mismatch "}

        var splitAmount = mutableMapOf<User, Money>()

        splitMap.forEach { (user, f) ->
            splitAmount[user] = Money.paise ((totalAmount.paise * f)/100)
        }

        val totalAllocated = Money.paise(splitAmount.values.sumOf { it -> it.paise })
        val rem = totalAmount - totalAllocated

        return allocateRemainder(splitAmount, rem, expenseId)
    }

}