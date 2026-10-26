package lld.rippling.expenserules.followup

import lld.rippling.expenserules.entity.Expense
import lld.rippling.expenserules.entity.Violation
import lld.rippling.expenserules.rule.GroupBy
import lld.rippling.expenserules.rule.RuleSet

class IncrementalRulesEngine(ruleSet: RuleSet) {
    private val index = RuleIndex(ruleSet.expenseRules)
    private val groupRules = ruleSet.groupRules.groupBy { it.groupBy }
    private val groups = HashMap<Pair<GroupBy, String>, ArrayList<Expense>>()
    private val open = LinkedHashMap<Pair<String, String>, Violation>()

    val openViolations: List<Violation>
        get() = open.values.toList()

    fun add(expense: Expense): List<Violation> {
        val raised = ArrayList<Violation>()

        for (rule in index.candidates(expense)) {
            rule.check(expense)?.let { raised += it }
        }

        for ((groupBy, rules) in groupRules) {
            val groupId = groupBy.keyOf(expense)
            val members = groups.getOrPut(groupBy to groupId) { ArrayList() }
            members += expense
            for (rule in rules) {
                val violation = rule.check(groupId, members) ?: continue
                val id = rule.name to groupId
                if (!open.containsKey(id)) {
                    raised += violation
                }
                open[id] = violation
            }
        }

        for (violation in raised) {
            open.putIfAbsent(violation.ruleName to violation.targetId, violation)
        }
        return raised
    }
}
