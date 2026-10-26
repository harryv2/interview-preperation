package lld.rippling.expenserules.rule

import lld.rippling.expenserules.entity.Expense
import lld.rippling.expenserules.entity.Severity
import lld.rippling.expenserules.entity.Violation

class AllOfRule(
    override val name: String,
    private val rules: List<ExpenseRule>,
    override val severity: Severity = Severity.BLOCK,
) : ExpenseRule {

    init {
        require(rules.isNotEmpty()) { "Composite rule $name needs children" }
    }

    override fun check(expense: Expense): Violation? {
        val hits = ArrayList<Violation>()
        for (rule in rules) {
            val hit = rule.check(expense) ?: return null
            hits += hit
        }
        return Violation(name, expense.id, hits.joinToString(" AND ") { it.message }, severity)
    }
}

class AnyOfRule(
    override val name: String,
    private val rules: List<ExpenseRule>,
    override val severity: Severity = Severity.BLOCK,
) : ExpenseRule {

    init {
        require(rules.isNotEmpty()) { "Composite rule $name needs children" }
    }

    override fun check(expense: Expense): Violation? {
        val hits = rules.mapNotNull { it.check(expense) }
        if (hits.isEmpty()) {
            return null
        }
        return Violation(name, expense.id, hits.joinToString(" OR ") { it.message }, severity)
    }
}
