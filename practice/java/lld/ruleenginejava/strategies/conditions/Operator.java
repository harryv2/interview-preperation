package lld.ruleenginejava.strategies.conditions;

public enum Operator {
    GT,
    LT,
    EQ;

    public boolean test(Object actual, Object expected) {
        return switch (this) {
            case GT -> compare(actual, expected) > 0;
            case LT -> compare(actual, expected) < 0;
            case EQ -> actual instanceof Number && expected instanceof Number
                    ? compare(actual, expected) == 0
                    : actual.equals(expected);
        };
    }

    private int compare(Object actual, Object expected) {
        if (!(actual instanceof Number a) || !(expected instanceof Number e)) {
            throw new IllegalArgumentException(this + " needs numbers, got " + actual + " and " + expected);
        }
        return Double.compare(a.doubleValue(), e.doubleValue());
    }
}
