package lld.ruleenginejava.strategies.selection;

import lld.ruleenginejava.entity.Facts;
import lld.ruleenginejava.entity.Rule;

import java.util.List;

@FunctionalInterface
public interface RuleSelectionStrategy {
    List<Rule> select(List<Rule> candidates, Facts facts);
}
