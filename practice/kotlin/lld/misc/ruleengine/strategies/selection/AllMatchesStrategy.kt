package lld.misc.ruleengine.strategies.selection

import lld.misc.ruleengine.entity.Facts
import lld.misc.ruleengine.entity.Rule

class AllMatchesStrategy : RuleSelectionStrategy {
    override fun select(candidates: List<Rule>, facts: Facts): List<Rule> {
        return candidates.filter { it.condition.evaluate(facts) }
    }
}
