package lld.rippling.expenserules.rule

import lld.rippling.expenserules.entity.Expense
import lld.rippling.expenserules.entity.Severity
import lld.rippling.expenserules.entity.Violation
import java.math.BigDecimal

class GroupTotalLimitRule(
    override val groupBy: GroupBy,
    private val limit: BigDecimal,
    override val severity: Severity = Severity.BLOCK,
) : GroupRule {
    override val name = "${groupBy.name.lowercase()}_total_limit"

    override fun check(groupId: String, expenses: List<Expense>): Violation? {
        val total = expenses.fold(BigDecimal.ZERO) { sum, expense -> sum + expense.amount }
        if (total <= limit) {
            return null
        }
        return Violation(name, groupId, "${groupBy.name.lowercase()} total $total is over $limit", severity)
    }
}

class GroupExpenseTypeLimitRule(
    override val groupBy: GroupBy,
    private val expenseType: String,
    private val limit: BigDecimal,
    override val severity: Severity = Severity.BLOCK,
) : GroupRule {
    override val name = "${groupBy.name.lowercase()}_${expenseType}_limit"

    override fun check(groupId: String, expenses: List<Expense>): Violation? {
        val total = expenses
            .filter { it.expenseType == expenseType }
            .fold(BigDecimal.ZERO) { sum, expense -> sum + expense.amount }
        if (total <= limit) {
            return null
        }
        return Violation(name, groupId, "$expenseType total $total is over $limit", severity)
    }
}
