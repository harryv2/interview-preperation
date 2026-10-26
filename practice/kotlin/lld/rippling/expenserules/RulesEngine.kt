package lld.rippling.expenserules

import lld.rippling.expenserules.entity.Expense
import lld.rippling.expenserules.entity.Severity
import lld.rippling.expenserules.entity.Violation
import lld.rippling.expenserules.followup.RuleIndex
import lld.rippling.expenserules.rule.ExpenseRule
import lld.rippling.expenserules.rule.GroupRule
import lld.rippling.expenserules.rule.RuleSet

class Report(val violations: List<Violation>) {

    val blocked: Boolean
        get() = violations.any { it.severity == Severity.BLOCK }

    fun forTarget(targetId: String): List<Violation> {
        return violations.filter { it.targetId == targetId }
    }
}

class RulesEngine(
    private val expenseRules: List<ExpenseRule>,
    private val groupRules: List<GroupRule>,
) {

    constructor(ruleSet: RuleSet) : this(ruleSet.expenseRules, ruleSet.groupRules)

    private val index = RuleIndex(expenseRules)

    fun evaluate(expenses: List<Expense>): Report {
        val violations = ArrayList<Violation>()

        for (expense in expenses) {
            for (rule in index.candidates(expense)) {
                rule.check(expense)?.let { violations += it }
            }
        }

        val rulesByGrouping = groupRules.groupBy { it.groupBy }
        for ((groupBy, rules) in rulesByGrouping) {
            val groups = expenses.groupBy(groupBy.keyOf)
            for ((groupId, groupExpenses) in groups) {
                for (rule in rules) {
                    rule.check(groupId, groupExpenses)?.let { violations += it }
                }
            }
        }

        return Report(violations)
    }
}
