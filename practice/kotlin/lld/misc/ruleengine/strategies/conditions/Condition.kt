package lld.misc.ruleengine.strategies.conditions

import lld.misc.ruleengine.entity.Facts

sealed interface Condition {
    fun evaluate(facts: Facts): Boolean
}
