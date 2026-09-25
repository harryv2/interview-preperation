package lld.misc.ruleengine.strategies.selection

import lld.misc.ruleengine.entity.Facts
import lld.misc.ruleengine.entity.Rule

fun interface RuleSelectionStrategy {
    fun select(candidates: List<Rule>, facts: Facts): List<Rule>
}
