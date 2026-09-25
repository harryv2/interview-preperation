package lld.misc.ruleengine.strategies.actions

import lld.misc.ruleengine.entity.Facts

fun interface Action {
    fun execute(facts: Facts): Effect
}
