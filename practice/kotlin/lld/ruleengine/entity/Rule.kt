package lld.ruleengine.entity

import lld.ruleengine.strategies.actions.Action
import lld.ruleengine.strategies.conditions.Condition

data class Rule(
    val id: String,
    val priority: Int,
    val condition: Condition,
    val actions: List<Action>,
)
