package lld.ruleengine.strategies.selection

import lld.ruleengine.entity.Facts
import lld.ruleengine.entity.Rule

class FirstMatchStrategy : RuleSelectionStrategy {
    override fun select(candidates: List<Rule>, facts: Facts): List<Rule> {
        val first = candidates.firstOrNull { it.condition.evaluate(facts) }
        return listOfNotNull(first)
    }
}
