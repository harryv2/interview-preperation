package lld_self.splitwise.strategies.split

import lld_self.splitwise.entity.Money
import lld_self.splitwise.entity.User
import kotlin.uuid.Uuid

interface SplitStrategy {
    val participants: Set<User>
    fun getOwed(totalAmount: Money, expenseId: Uuid): Map<User, Money>
}


internal fun allocateRemainder(
    shares: Map<User, Money>,
    remainder: Money,
    expenseId: Uuid
): Map<User, Money> {
    require(shares.isNotEmpty()) { "Cannot split between zero people" }

    val entries = shares.toList()
    val start = expenseId.hashCode().mod(entries.size)
    val rotated = entries.drop(start) + entries.take(start)

    return rotated.mapIndexed { index, (user, money) ->
        val extra = if (index < remainder.paise) Money.paise(1) else Money.ZERO
        user to money + extra
    }.toMap()
}