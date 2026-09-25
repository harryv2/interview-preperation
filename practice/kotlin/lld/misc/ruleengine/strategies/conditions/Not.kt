package lld.misc.ruleengine.strategies.conditions

import lld.misc.ruleengine.entity.Facts

class Not(private val child: Condition) : Condition {
    override fun evaluate(facts: Facts): Boolean {
        return !child.evaluate(facts)
    }
}
