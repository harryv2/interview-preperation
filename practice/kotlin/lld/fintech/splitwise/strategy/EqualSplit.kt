package lld.fintech.splitwise.strategy

import lld.fintech.splitwise.entity.Money
import lld.fintech.splitwise.entity.User

/** Everyone pays the same, leftover paise distributed deterministically. */
class EqualSplit(members: Collection<User>) : SplitStrategy {

    override val participants: Set<User> = members.toSet()
    private val ordered: List<User> = participants.sortedBy { it.id }

    init {
        require(ordered.isNotEmpty()) { "Equal split needs at least one participant" }
    }

    override fun computeOwedBy(total: Money, expenseId: String): Map<User, Money> {
        val n = ordered.size
        val base = total.paise / n
        val remainder = total.paise % n
        return allocateRemainder(ordered.map { it to base }, remainder, expenseId)
    }
}
