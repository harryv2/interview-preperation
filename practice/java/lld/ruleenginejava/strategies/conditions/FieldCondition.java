package lld.ruleenginejava.strategies.conditions;

import lld.ruleenginejava.entity.Facts;

public record FieldCondition(String field, Operator op, Object value) implements Condition {
    @Override
    public boolean evaluate(Facts facts) {
        Object actual = facts.get(field);
        if (actual == null) {
            return false;
        }
        return op.test(actual, value);
    }
}
