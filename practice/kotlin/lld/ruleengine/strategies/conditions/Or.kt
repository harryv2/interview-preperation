package lld.ruleengine.strategies.conditions

import lld.ruleengine.entity.Facts

class Or(private val children: List<Condition>) : Condition {
    override fun evaluate(facts: Facts): Boolean {
        return children.any { it.evaluate(facts) }
    }
}
