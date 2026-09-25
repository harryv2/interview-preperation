package lld.fintech.splitwise.strategy

import lld.fintech.splitwise.entity.Money
import lld.fintech.splitwise.entity.User

/**
 * Percentages in basis points: 10000 = 100.00%, so 33.33% is 3333.
 * Integers again, for the same reason Money is an integer.
 */
class PercentSplit(private val basisPoints: Map<User, Int>) : SplitStrategy {

    companion object {
        const val FULL = 10_000
        fun of(percentages: Map<User, Double>) =
            PercentSplit(percentages.mapValues { Math.round(it.value * 100).toInt() })
    }

    override val participants: Set<User> = basisPoints.keys

    init {
        require(basisPoints.isNotEmpty()) { "Percent split needs at least one participant" }
        val sum = basisPoints.values.sum()
        require(sum == FULL) {
            "Percentages add up to ${sum / 100.0}%, they must add up to 100%"
        }
    }

    override fun computeOwedBy(total: Money, expenseId: String): Map<User, Money> {
        val ordered = participants.sortedBy { it.id }
        val shares = ordered.map { user ->
            user to (total.paise * basisPoints.getValue(user)) / FULL
        }
        val allocated = shares.sumOf { it.second }
        return allocateRemainder(shares, total.paise - allocated, expenseId)
    }
}
