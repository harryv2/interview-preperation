package lld.ruleenginejava.strategies.conditions;

import lld.ruleenginejava.entity.Facts;

import java.util.List;

public record Or(List<Condition> children) implements Condition {
    @Override
    public boolean evaluate(Facts facts) {
        return children.stream().anyMatch(c -> c.evaluate(facts));
    }
}
