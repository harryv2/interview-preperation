package lld.ruleengine.strategies.selection

import lld.ruleengine.entity.Facts
import lld.ruleengine.entity.Rule

class AllMatchesStrategy : RuleSelectionStrategy {
    override fun select(candidates: List<Rule>, facts: Facts): List<Rule> {
        return candidates.filter { it.condition.evaluate(facts) }
    }
}
