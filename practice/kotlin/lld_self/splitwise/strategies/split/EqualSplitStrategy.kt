package lld_self.splitwise.strategies.split

import lld_self.splitwise.entity.Money
import lld_self.splitwise.entity.User
import kotlin.uuid.Uuid

class EqualSplitStrategy(
    override val participants: Set<User>
) : SplitStrategy {

    override fun getOwed(
        totalAmount: Money,
        expenseId: Uuid
    ): Map<User, Money> {

        val splitAmount = mutableMapOf<User, Money>()

        participants.forEach {
            splitAmount[it] = Money.paise(totalAmount.paise/ participants.size)
        }

        val totalAllocated = Money.paise(splitAmount.values.sumOf { it.paise })
        val rem = totalAmount - totalAllocated

        return allocateRemainder(splitAmount, rem, expenseId)
    }

}