package lld.ruleenginejava.strategies.conditions;

import java.util.List;

public final class Conditions {
    private Conditions() {
    }

    public static Condition gt(String field, Number value) {
        return new FieldCondition(field, Operator.GT, value);
    }

    public static Condition lt(String field, Number value) {
        return new FieldCondition(field, Operator.LT, value);
    }

    public static Condition eq(String field, Object value) {
        return new FieldCondition(field, Operator.EQ, value);
    }

    public static Condition and(Condition... children) {
        return new And(List.of(children));
    }

    public static Condition or(Condition... children) {
        return new Or(List.of(children));
    }

    public static Condition not(Condition child) {
        return new Not(child);
    }
}
