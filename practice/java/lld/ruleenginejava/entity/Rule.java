package lld.ruleenginejava.entity;

import lld.ruleenginejava.strategies.actions.Action;
import lld.ruleenginejava.strategies.conditions.Condition;

import java.util.List;

public record Rule(String id, int priority, Condition condition, List<Action> actions) {
}
