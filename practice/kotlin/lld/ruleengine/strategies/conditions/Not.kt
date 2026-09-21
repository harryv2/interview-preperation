package lld.ruleengine.strategies.conditions

import lld.ruleengine.entity.Facts

class Not(private val child: Condition) : Condition {
    override fun evaluate(facts: Facts): Boolean {
        return !child.evaluate(facts)
    }
}
