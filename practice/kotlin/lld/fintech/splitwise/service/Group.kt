package lld.fintech.splitwise.service

import lld.fintech.splitwise.entity.Expense
import lld.fintech.splitwise.entity.Money
import lld.fintech.splitwise.entity.Transfer
import lld.fintech.splitwise.entity.User
import lld.fintech.splitwise.simplifier.DebtSimplifier
import lld.fintech.splitwise.simplifier.GreedySimplifier
import java.util.UUID

// =====================================================================
//  GROUP
//
//  THE central decision of this design: expenses are the truth,
//  balances are computed.
//
//  Storing running balances makes editing impossible. A balance is a
//  sum with the addends thrown away, so when an expense changes you
//  have no way to unwind its contribution.
//
//  Recomputing is cached, and every mutation invalidates. That is only
//  safe because all three mutators go through this class.
// =====================================================================

class Group(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    members: Collection<User>,
    var isSimplified: Boolean = false,
    private val simplifier: DebtSimplifier = GreedySimplifier()
) {

    private val memberSet: MutableSet<User> = members.toMutableSet()
    val members: Set<User> get() = memberSet

    private val expenseById = LinkedHashMap<String, Expense>()
    private var cachedBalances: Map<User, Money>? = null

    fun addMember(user: User) {
        memberSet.add(user)
    }

    // ---------- mutation: every path invalidates the cache ----------

    fun addExpense(expense: Expense): Expense {
        validate(expense)
        expenseById[expense.id] = expense
        cachedBalances = null
        return expense
    }

    fun updateExpense(expense: Expense) {
        require(expenseById.containsKey(expense.id)) { "No expense with id ${expense.id}" }
        validate(expense)
        expenseById[expense.id] = expense
        cachedBalances = null
    }

    fun deleteExpense(expenseId: String) {
        if (expenseById.remove(expenseId) != null) {
            cachedBalances = null
        }
    }

    fun expenses(): List<Expense> = expenseById.values.toList()

    private fun validate(expense: Expense) {
        val outsiders = expense.participants - memberSet
        require(outsiders.isEmpty()) {
            "Not members of $name: ${outsiders.joinToString { it.name }}"
        }
    }

    // ---------- derived state ----------

    fun balances(): Map<User, Money> =
        cachedBalances ?: computeBalances().also { cachedBalances = it }

    fun balanceOf(user: User): Money = balances()[user] ?: Money.ZERO

    private fun computeBalances(): Map<User, Money> {
        val net = LinkedHashMap<User, Long>()
        for (expense in expenseById.values) {
            for (user in expense.participants) {
                net[user] = (net[user] ?: 0L) + expense.netFor(user).paise
            }
        }

        // Every expense contributes sum(paidBy) - sum(owedBy) = 0, so the
        // group total is always zero. If it isn't, an expense was built
        // with a broken split and the bug is upstream of here.
        check(net.values.sum() == 0L) {
            "Balances for $name do not sum to zero. Some expense has a broken split."
        }

        return net.filterValues { it != 0L }.mapValues { Money(it.value) }
    }

    /** What the app shows on the "settle up" screen. */
    fun settlementPlan(): List<Transfer> =
        if (isSimplified) simplifier.simplify(balances()) else rawPairwiseDebts()

    /**
     * The unsimplified view: who owes whom, netted per pair only, never
     * across the group. Each expense is matched internally, then the
     * per-pair totals are netted.
     */
    private fun rawPairwiseDebts(): List<Transfer> {
        val perExpenseMatcher = GreedySimplifier()
        // key is the pair ordered by id; value is what the first owes the second
        val ledger = LinkedHashMap<Pair<User, User>, Long>()

        for (expense in expenseById.values) {
            val localNet = expense.participants.associateWith { expense.netFor(it) }
            for (transfer in perExpenseMatcher.simplify(localNet)) {
                val forward = transfer.from.id < transfer.to.id
                val key = if (forward) transfer.from to transfer.to else transfer.to to transfer.from
                val signed = if (forward) transfer.amount.paise else -transfer.amount.paise
                ledger[key] = (ledger[key] ?: 0L) + signed
            }
        }

        return ledger.entries
            .filter { it.value != 0L }
            .map { (pair, amount) ->
                val (first, second) = pair
                if (amount > 0) Transfer(first, second, Money(amount))
                else Transfer(second, first, Money(-amount))
            }
            .sortedWith(compareBy({ it.from.id }, { it.to.id }))
    }
}
