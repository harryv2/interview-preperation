package lld.misc.ruleengine.entity

import lld.misc.ruleengine.strategies.actions.Effect

data class RuleResult(
    val matched: List<Rule>,
    val effects: List<Effect>,
)
