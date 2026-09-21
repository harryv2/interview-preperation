package lld.ruleenginejava.strategies.conditions;

import lld.ruleenginejava.entity.Facts;

import java.util.List;

public record And(List<Condition> children) implements Condition {
    @Override
    public boolean evaluate(Facts facts) {
        return children.stream().allMatch(c -> c.evaluate(facts));
    }
}
