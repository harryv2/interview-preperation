package lld.misc.ruleengine.strategies.conditions

import lld.misc.ruleengine.entity.Facts

class FieldCondition(val field: String, val op: Operator, val value: Any) : Condition {
    override fun evaluate(facts: Facts): Boolean {
        val actual = facts[field] ?: return false
        return op.test(actual, value)
    }
}
