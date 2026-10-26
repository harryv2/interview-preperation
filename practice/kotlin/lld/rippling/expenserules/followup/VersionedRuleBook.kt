package lld.rippling.expenserules.followup

import lld.rippling.expenserules.Report
import lld.rippling.expenserules.RulesEngine
import lld.rippling.expenserules.entity.Expense
import lld.rippling.expenserules.rule.RuleSet
import java.time.LocalDate
import java.util.TreeMap

class VersionedRuleBook {
    private val versions = TreeMap<LocalDate, RulesEngine>()

    fun publish(effectiveFrom: LocalDate, ruleSet: RuleSet) {
        versions[effectiveFrom] = RulesEngine(ruleSet)
    }

    fun versionOn(date: LocalDate): LocalDate? {
        return versions.floorKey(date)
    }

    fun evaluate(expenses: List<Expense>): Report {
        val byVersion = expenses.groupBy { requireNotNull(versionOn(it.date)) { "No rules live on ${it.date}" } }
        val violations = byVersion.flatMap { (version, batch) ->
            versions.getValue(version).evaluate(batch).violations
        }
        return Report(violations)
    }
}
