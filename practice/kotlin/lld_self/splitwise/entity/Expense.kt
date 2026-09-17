package lld_self.splitwise.entity

import lld_self.splitwise.strategies.split.ExactSplitStrategy
import lld_self.splitwise.strategies.split.SplitStrategy
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid


enum class ExpenseType {
    REGULAR,
    SETTLEMENT
}

class Expense(
    val title: String,
    val description: String,
    val type: ExpenseType = ExpenseType.REGULAR,
    val amount: Money,
    val paidBy: Map<User, Money>,
    val splitStrategy: SplitStrategy,
) {

    val id: Uuid = Uuid.random()
    val createdAt: Instant = Clock.System.now()


    init {
        require(amount > Money.ZERO) { "Amount greater than 0" }

        require(paidBy.isNotEmpty()) { "Paid by not empty" }

        val sum = paidBy.values.sumOf { it.paise }

        require(sum == amount.paise) { "Total paid mismatch" }

    }


    val owedBy: Map<User, Money> = splitStrategy.getOwed(amount, id)
    val participants = splitStrategy.participants + paidBy.keys

    init {
        val owedSum = owedBy.values.sumOf { it.paise }

        require(owedSum == amount.paise) { "Total owed mismatch" }
    }

    fun netFor(user: User): Money {
        return paidBy.getOrDefault(user, Money.ZERO) - owedBy.getOrDefault(user, Money.ZERO)
    }


    companion object {
        fun Regular(
            title: String,
            description: String,
            amount: Money,
            paidBy: Map<User, Money>,
            splitStrategy: SplitStrategy,
        ): Expense {
            return Expense(
                title,
                description,
                ExpenseType.REGULAR,
                amount,
                paidBy,
                splitStrategy
            )
        }


        fun Settlement(
            from: User,
            amount: Money,
            to: User
        ): Expense {
            return Expense(
                "Settled",
                "Settlement from ${from.name} to ${to.name}",
                ExpenseType.SETTLEMENT,
                amount,
                paidBy = mapOf(from to amount),
                splitStrategy = ExactSplitStrategy(mapOf(to to amount))
            )
        }

    }
}