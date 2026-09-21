package lld.ruleengine.strategies.actions

import lld.ruleengine.entity.Facts

fun interface Action {
    fun execute(facts: Facts): Effect
}
