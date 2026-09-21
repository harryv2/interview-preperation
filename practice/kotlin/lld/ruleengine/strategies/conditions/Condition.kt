package lld.ruleengine.strategies.conditions

import lld.ruleengine.entity.Facts

sealed interface Condition {
    fun evaluate(facts: Facts): Boolean
}
