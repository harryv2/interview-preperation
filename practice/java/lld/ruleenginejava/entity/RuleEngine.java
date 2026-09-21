package lld.ruleenginejava.entity;

import lld.ruleenginejava.strategies.actions.Action;
import lld.ruleenginejava.strategies.actions.Effect;
import lld.ruleenginejava.strategies.selection.AllMatchesStrategy;
import lld.ruleenginejava.strategies.selection.RuleSelectionStrategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RuleEngine {
    private final RuleSelectionStrategy selection;
    private final List<Rule> rules = new CopyOnWriteArrayList<>();

    public RuleEngine() {
        this(new AllMatchesStrategy());
    }

    public RuleEngine(RuleSelectionStrategy selection) {
        this.selection = selection;
    }

    public void add(Rule rule) {
        boolean exists = rules.stream().anyMatch(r -> r.id().equals(rule.id()));
        if (exists) {
            throw new IllegalArgumentException("Rule " + rule.id() + " already exists");
        }
        rules.add(rule);
    }

    public void remove(String id) {
        rules.removeIf(r -> r.id().equals(id));
    }

    public RuleResult evaluate(Facts facts) {
        List<Rule> byPriority = rules.stream()
                .sorted(Comparator.comparingInt(Rule::priority).reversed())
                .toList();
        List<Rule> selected = selection.select(byPriority, facts);

        List<Effect> effects = new ArrayList<>();
        for (Rule rule : selected) {
            for (Action action : rule.actions()) {
                effects.add(action.execute(facts));
            }
        }
        return new RuleResult(selected, effects);
    }
}
