package lld.misc.ruleengine

import lld.misc.ruleengine.entity.Facts
import lld.misc.ruleengine.entity.Rule
import lld.misc.ruleengine.entity.RuleEngine
import lld.misc.ruleengine.strategies.actions.Action
import lld.misc.ruleengine.strategies.actions.Effect
import lld.misc.ruleengine.strategies.conditions.and
import lld.misc.ruleengine.strategies.conditions.eq
import lld.misc.ruleengine.strategies.conditions.gt
import lld.misc.ruleengine.strategies.conditions.lt
import lld.misc.ruleengine.strategies.conditions.not
import lld.misc.ruleengine.strategies.conditions.or
import lld.misc.ruleengine.strategies.selection.AllMatchesStrategy
import lld.misc.ruleengine.strategies.selection.FirstMatchStrategy

data class Notify(val message: String) : Effect
data class AdjustBid(val percent: Int) : Effect
object PauseCampaign : Effect {
    override fun toString(): String = "PauseCampaign"
}

fun main() {
    val rules = listOf(
        Rule(
            id = "overspend",
            priority = 100,
            condition = ("spend" gt 10_000) and not("status" eq "PAUSED"),
            actions = listOf(Action { PauseCampaign }, Action { Notify("spend ${it["spend"]} over budget") }),
        ),
        Rule(
            id = "expensive-clicks",
            priority = 50,
            condition = ("cpc" gt 10) and (("ctr" gt 0) or ("views" lt 100)),
            actions = listOf(Action { AdjustBid(-10) }),
        ),
        Rule(
            id = "strong-performer",
            priority = 10,
            condition = ("ctr" gt 5) and ("cpc" lt 2),
            actions = listOf(Action { AdjustBid(20) }),
        ),
    )
    val allMatches = RuleEngine(AllMatchesStrategy())
    val firstMatch = RuleEngine(FirstMatchStrategy())
    rules.forEach {
        allMatches.add(it)
        firstMatch.add(it)
    }

    val campaigns = mapOf(
        "campaign-A" to Facts(mapOf("spend" to 12_500, "cpc" to 12.5, "ctr" to 1.2, "views" to 5000, "status" to "ACTIVE")),
        "campaign-B" to Facts(mapOf("spend" to 300, "cpc" to 1.1, "ctr" to 6.4, "views" to 20_000, "status" to "ACTIVE")),
        "campaign-C" to Facts(mapOf("spend" to 900, "cpc" to 15.0, "ctr" to 0.0, "views" to 40, "status" to "ACTIVE")),
    )

    for ((name, facts) in campaigns) {
        val all = allMatches.evaluate(facts)
        val first = firstMatch.evaluate(facts)
        println("$name")
        println("  all matches   -> ${all.matched.map { it.id }} effects ${all.effects}")
        println("  first match   -> ${first.matched.map { it.id }} effects ${first.effects}")
    }
}
