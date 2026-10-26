package lld.rippling.expenserules.followup

import lld.rippling.expenserules.entity.Expense
import lld.rippling.expenserules.rule.ExpenseRule
import lld.rippling.expenserules.rule.Field
import lld.rippling.expenserules.rule.Trigger

class RuleIndex(rules: List<ExpenseRule>) {
    private val byTrigger = HashMap<Trigger, ArrayList<ExpenseRule>>()
    private val always = ArrayList<ExpenseRule>()

    init {
        for (rule in rules) {
            val trigger = rule.trigger
            if (trigger == null) {
                always += rule
            } else {
                byTrigger.getOrPut(trigger) { ArrayList() }.add(rule)
            }
        }
    }

    fun candidates(expense: Expense): List<ExpenseRule> {
        val result = ArrayList(always)
        for (field in Field.entries) {
            byTrigger[Trigger(field, field.valueOf(expense))]?.let { result += it }
        }
        return result
    }
}
