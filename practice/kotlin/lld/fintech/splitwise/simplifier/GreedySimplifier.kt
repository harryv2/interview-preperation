package lld.fintech.splitwise.simplifier

import lld.fintech.splitwise.entity.Money
import lld.fintech.splitwise.entity.Transfer
import lld.fintech.splitwise.entity.User
import java.util.PriorityQueue

/**
 * Repeatedly match the largest creditor against the largest debtor and
 * settle the smaller of the two. Each round fully zeroes at least one
 * person, so it runs at most n-1 times.
 *
 * O(n log n), at most n-1 transfers.
 *
 * This is a heuristic, NOT the true minimum. The exact minimum requires
 * finding the maximum number of disjoint zero-sum subgroups, which is
 * subset-sum and therefore NP-hard. Real apps use greedy.
 */
class GreedySimplifier : DebtSimplifier {

    private data class Holding(val user: User, val amount: Long)

    override fun simplify(balances: Map<User, Money>): List<Transfer> {

        // The thenBy is not decoration. Without a tie-break, two people
        // owing the same amount come out in arbitrary order and the same
        // group produces a different plan on two consecutive loads.
        val order = compareByDescending<Holding> { it.amount }.thenBy { it.user.id }

        val creditors = PriorityQueue(order)
        val debtors = PriorityQueue(order)

        for ((user, money) in balances) {
            when {
                money.paise > 0 -> creditors.add(Holding(user, money.paise))
                money.paise < 0 -> debtors.add(Holding(user, -money.paise))
                // a zero balance never enters either queue
            }
        }

        val transfers = mutableListOf<Transfer>()

        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val creditor = creditors.poll()
            val debtor = debtors.poll()

            val settled = minOf(creditor.amount, debtor.amount)
            transfers.add(Transfer(debtor.user, creditor.user, Money(settled)))

            if (creditor.amount > settled) {
                creditors.add(Holding(creditor.user, creditor.amount - settled))
            }
            if (debtor.amount > settled) {
                debtors.add(Holding(debtor.user, debtor.amount - settled))
            }
        }

        return transfers
    }
}
