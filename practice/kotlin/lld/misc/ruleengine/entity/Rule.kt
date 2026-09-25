package lld.misc.ruleengine.entity

import lld.misc.ruleengine.strategies.actions.Action
import lld.misc.ruleengine.strategies.conditions.Condition

data class Rule(
    val id: String,
    val priority: Int,
    val condition: Condition,
    val actions: List<Action>,
)
