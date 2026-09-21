package lld.ruleenginejava.strategies.conditions;

import lld.ruleenginejava.entity.Facts;

public record Not(Condition child) implements Condition {
    @Override
    public boolean evaluate(Facts facts) {
        return !child.evaluate(facts);
    }
}
