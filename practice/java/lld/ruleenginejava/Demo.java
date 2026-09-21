package lld.ruleenginejava;

import lld.ruleenginejava.entity.Facts;
import lld.ruleenginejava.entity.Rule;
import lld.ruleenginejava.entity.RuleEngine;
import lld.ruleenginejava.entity.RuleResult;
import lld.ruleenginejava.strategies.actions.Effect;
import lld.ruleenginejava.strategies.selection.AllMatchesStrategy;
import lld.ruleenginejava.strategies.selection.FirstMatchStrategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static lld.ruleenginejava.strategies.conditions.Conditions.and;
import static lld.ruleenginejava.strategies.conditions.Conditions.eq;
import static lld.ruleenginejava.strategies.conditions.Conditions.gt;
import static lld.ruleenginejava.strategies.conditions.Conditions.lt;
import static lld.ruleenginejava.strategies.conditions.Conditions.not;
import static lld.ruleenginejava.strategies.conditions.Conditions.or;

public class Demo {

    record Notify(String message) implements Effect {
    }

    record AdjustBid(int percent) implements Effect {
    }

    record PauseCampaign() implements Effect {
    }

    public static void main(String[] args) {
        List<Rule> rules = List.of(
                new Rule("overspend", 100,
                        and(gt("spend", 10_000), not(eq("status", "PAUSED"))),
                        List.of(f -> new PauseCampaign(), f -> new Notify("spend " + f.get("spend") + " over budget"))),
                new Rule("expensive-clicks", 50,
                        and(gt("cpc", 10), or(gt("ctr", 0), lt("views", 100))),
                        List.of(f -> new AdjustBid(-10))),
                new Rule("strong-performer", 10,
                        and(gt("ctr", 5), lt("cpc", 2)),
                        List.of(f -> new AdjustBid(20)))
        );
        RuleEngine allMatches = new RuleEngine(new AllMatchesStrategy());
        RuleEngine firstMatch = new RuleEngine(new FirstMatchStrategy());
        for (Rule rule : rules) {
            allMatches.add(rule);
            firstMatch.add(rule);
        }

        Map<String, Facts> campaigns = new LinkedHashMap<>();
        campaigns.put("campaign-A", new Facts(Map.of("spend", 12_500, "cpc", 12.5, "ctr", 1.2, "views", 5000, "status", "ACTIVE")));
        campaigns.put("campaign-B", new Facts(Map.of("spend", 300, "cpc", 1.1, "ctr", 6.4, "views", 20_000, "status", "ACTIVE")));
        campaigns.put("campaign-C", new Facts(Map.of("spend", 900, "cpc", 15.0, "ctr", 0.0, "views", 40, "status", "ACTIVE")));

        for (Map.Entry<String, Facts> entry : campaigns.entrySet()) {
            RuleResult all = allMatches.evaluate(entry.getValue());
            RuleResult first = firstMatch.evaluate(entry.getValue());
            System.out.println(entry.getKey());
            System.out.println("  all matches   -> " + ids(all) + " effects " + all.effects());
            System.out.println("  first match   -> " + ids(first) + " effects " + first.effects());
        }
    }

    private static List<String> ids(RuleResult result) {
        return result.matched().stream().map(Rule::id).toList();
    }
}
