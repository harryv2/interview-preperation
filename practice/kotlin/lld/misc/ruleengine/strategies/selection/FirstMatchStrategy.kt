package lld.misc.ruleengine.strategies.selection

import lld.misc.ruleengine.entity.Facts
import lld.misc.ruleengine.entity.Rule

class FirstMatchStrategy : RuleSelectionStrategy {
    override fun select(candidates: List<Rule>, facts: Facts): List<Rule> {
        val first = candidates.firstOrNull { it.condition.evaluate(facts) }
        return listOfNotNull(first)
    }
}
