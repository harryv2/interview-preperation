package lld.fintech.splitwise.strategy

import lld.fintech.splitwise.entity.Money
import lld.fintech.splitwise.entity.User
import kotlin.math.absoluteValue

// =====================================================================
//  SPLIT STRATEGIES
//
//  Step 5 of the recipe: "how is this bill divided" has several valid
//  answers, so it is an interface.
//
//  Each strategy takes its own input in the CONSTRUCTOR rather than in
//  the method. ExactSplit accepts a rupee map, PercentSplit accepts
//  basis points. You cannot hand one the other's data.
//
//  `expenseId` is passed to computeOwedBy so that leftover-paisa
//  allocation is deterministic AND varies between expenses. See
//  allocateRemainder below.
// =====================================================================

interface SplitStrategy {
    val participants: Set<User>
    fun computeOwedBy(total: Money, expenseId: String): Map<User, Money>
}

/**
 * Hands out the leftover paise that integer division loses.
 *
 * Rules:
 *  - deterministic: the same expense always produces the same result,
 *    so editing a title never silently moves a paisa to someone else.
 *  - rotating: the starting point depends on the expense id, so the
 *    same person is not always the one who absorbs it.
 *
 * `shares` must already be in a canonical order (sorted by user id).
 *
 * Visible to the rest of this package (not just this file) so the
 * EqualSplit and PercentSplit strategies can share it.
 */
internal fun allocateRemainder(
    shares: List<Pair<User, Long>>,
    remainder: Long,
    expenseId: String
): Map<User, Money> {
    require(shares.isNotEmpty()) { "Cannot split between zero people" }
    if (remainder == 0L) {
        return shares.associate { (user, amount) -> user to Money(amount) }
    }

    val n = shares.size
    // toLong() first: Int.MIN_VALUE.absoluteValue overflows back to itself.
    val start = (expenseId.hashCode().toLong().absoluteValue % n).toInt()

    val result = LinkedHashMap<User, Money>(n)
    shares.forEachIndexed { index, (user, amount) ->
        val positionFromStart = (index - start + n) % n
        val extra = if (positionFromStart < remainder) 1L else 0L
        result[user] = Money(amount + extra)
    }
    return result
}
