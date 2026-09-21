package lld.ruleenginejava.strategies.selection;

import lld.ruleenginejava.entity.Facts;
import lld.ruleenginejava.entity.Rule;

import java.util.List;

public class AllMatchesStrategy implements RuleSelectionStrategy {
    @Override
    public List<Rule> select(List<Rule> candidates, Facts facts) {
        return candidates.stream()
                .filter(r -> r.condition().evaluate(facts))
                .toList();
    }
}
