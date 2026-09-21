package lld.ruleengine.entity

import lld.ruleengine.strategies.actions.Effect

data class RuleResult(
    val matched: List<Rule>,
    val effects: List<Effect>,
)
