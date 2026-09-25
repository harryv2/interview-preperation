package lld.misc.ruleengine.entity

import lld.misc.ruleengine.strategies.selection.AllMatchesStrategy
import lld.misc.ruleengine.strategies.selection.RuleSelectionStrategy
import java.util.concurrent.CopyOnWriteArrayList

class RuleEngine(private val selection: RuleSelectionStrategy = AllMatchesStrategy()) {
    private val rules = CopyOnWriteArrayList<Rule>()

    fun add(rule: Rule) {
        require(rules.none { it.id == rule.id }) { "Rule ${rule.id} already exists" }
        rules += rule
    }

    fun remove(id: String) {
        rules.removeIf { it.id == id }
    }

    fun evaluate(facts: Facts): RuleResult {
        val byPriority = rules.sortedByDescending { it.priority }
        val selected = selection.select(byPriority, facts)
        val effects = selected.flatMap { rule -> rule.actions.map { it.execute(facts) } }
        return RuleResult(selected, effects)
    }
}
