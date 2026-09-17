package lld_self.splitwise.strategies.split

import lld_self.splitwise.entity.Money
import lld_self.splitwise.entity.User
import kotlin.uuid.Uuid

class ExactSplitStrategy(
    val splitMap: Map<User, Money>
) : SplitStrategy {

    override val participants: Set<User>
        get() = splitMap.keys

    override fun getOwned(
        totalAmount: Money,
        expenseId: Uuid
    ): Map<User, Money> {
        var total = Money.ZERO
        for (v in splitMap) {
            total += v.value
        }

        require(total == totalAmount) { "Total amount sum mismatch" }

        return splitMap
    }
}