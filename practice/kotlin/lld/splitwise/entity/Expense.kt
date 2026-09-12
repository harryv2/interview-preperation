package lld.splitwise.entity

import lld.splitwise.strategy.EqualSplit
import lld.splitwise.strategy.ExactSplit
import lld.splitwise.strategy.PercentSplit
import lld.splitwise.strategy.SplitStrategy
import java.util.UUID

// =====================================================================
//  EXPENSE
//
//  paidBy and owedBy are the same shape pointing opposite directions.
//  That is what makes multiple payers work without a special case, and
//  what makes a settlement just another expense.
//
//  The expense stores the split STRATEGY, not just the resulting map.
//  Without the rule, editing the amount is impossible -- you cannot
//  tell from a finished map whether it came from an equal or an exact
//  split.
// =====================================================================

enum class ExpenseType { REGULAR, SETTLEMENT }

class Expense(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val totalAmount: Money,
    val paidBy: Map<User, Money>,
    val splitStrategy: SplitStrategy,
    val type: ExpenseType = ExpenseType.REGULAR,
    val createdAtMillis: Long = System.currentTimeMillis()
) {

    // require = the caller got it wrong.
    init {
        require(totalAmount.paise > 0) { "Expense amount must be positive, got $totalAmount" }
        require(paidBy.isNotEmpty()) { "Somebody has to have paid" }
        val paid = paidBy.values.sumOf { it.paise }
        require(paid == totalAmount.paise) {
            "Payments add up to ${Money(paid)} but the total is $totalAmount"
        }
    }

    val owedBy: Map<User, Money> = splitStrategy.computeOwedBy(totalAmount, id)

    // check = our own code got it wrong. A strategy returned a bad map.
    init {
        val owed = owedBy.values.sumOf { it.paise }
        check(owed == totalAmount.paise) {
            "Split produced ${Money(owed)} but the total is $totalAmount"
        }
    }

    val participants: Set<User> get() = paidBy.keys + owedBy.keys

    /** Positive means this person is owed. Negative means they owe. */
    fun netFor(user: User): Money =
        (paidBy[user] ?: Money.ZERO) - (owedBy[user] ?: Money.ZERO)

    /**
     * Editing keeps the id, so the leftover-paisa allocation stays put
     * and Group.updateExpense can find the row to replace.
     */
    fun edited(
        title: String = this.title,
        description: String = this.description,
        totalAmount: Money = this.totalAmount,
        paidBy: Map<User, Money> = this.paidBy,
        splitStrategy: SplitStrategy = this.splitStrategy
    ) = Expense(id, title, description, totalAmount, paidBy, splitStrategy, type, createdAtMillis)

    override fun toString(): String = "$title ($totalAmount)"
}

/** Convenience builders. A settlement is an ordinary expense with one payer and one ower. */
object Expenses {

    fun equal(
        title: String,
        total: Money,
        paidBy: Map<User, Money>,
        participants: Collection<User>,
        description: String = "",
        id: String = UUID.randomUUID().toString()
    ) = Expense(id, title, description, total, paidBy, EqualSplit(participants))

    fun exact(
        title: String,
        total: Money,
        paidBy: Map<User, Money>,
        owed: Map<User, Money>,
        description: String = "",
        id: String = UUID.randomUUID().toString()
    ) = Expense(id, title, description, total, paidBy, ExactSplit(owed))

    fun percent(
        title: String,
        total: Money,
        paidBy: Map<User, Money>,
        percentages: Map<User, Double>,
        description: String = "",
        id: String = UUID.randomUUID().toString()
    ) = Expense(id, title, description, total, paidBy, PercentSplit.of(percentages))

    /**
     * Carol pays Alice 1000 in cash:
     *   paidBy = { Carol: 1000 }   Carol handed over money
     *   owedBy = { Alice: 1000 }   Alice received the value
     * netFor(Carol) = +1000, netFor(Alice) = -1000. Debt cancelled.
     * Zero new code in the balance calculation.
     */
    fun settlement(
        from: User,
        to: User,
        amount: Money,
        id: String = UUID.randomUUID().toString()
    ) = Expense(
        id = id,
        title = "Settlement",
        description = "${from.name} paid ${to.name}",
        totalAmount = amount,
        paidBy = mapOf(from to amount),
        splitStrategy = ExactSplit(mapOf(to to amount)),
        type = ExpenseType.SETTLEMENT
    )
}
