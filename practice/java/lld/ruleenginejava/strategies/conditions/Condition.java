package lld.ruleenginejava.strategies.conditions;

import lld.ruleenginejava.entity.Facts;

public sealed interface Condition permits FieldCondition, And, Or, Not {
    boolean evaluate(Facts facts);
}
