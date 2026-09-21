package lld.ruleengine.strategies.selection

import lld.ruleengine.entity.Facts
import lld.ruleengine.entity.Rule

fun interface RuleSelectionStrategy {
    fun select(candidates: List<Rule>, facts: Facts): List<Rule>
}
