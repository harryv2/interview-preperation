package lld.ruleenginejava.entity;

import lld.ruleenginejava.strategies.actions.Effect;

import java.util.List;

public record RuleResult(List<Rule> matched, List<Effect> effects) {
}
