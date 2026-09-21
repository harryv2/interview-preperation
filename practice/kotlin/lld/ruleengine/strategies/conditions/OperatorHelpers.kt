package lld.ruleengine.strategies.conditions

infix fun String.gt(value: Number): Condition {
    return FieldCondition(this, Operator.GT, value)
}

infix fun String.lt(value: Number): Condition {
    return FieldCondition(this, Operator.LT, value)
}

infix fun String.eq(value: Any): Condition {
    return FieldCondition(this, Operator.EQ, value)
}

infix fun Condition.and(other: Condition): Condition {
    return And(listOf(this, other))
}

infix fun Condition.or(other: Condition): Condition {
    return Or(listOf(this, other))
}

fun not(condition: Condition): Condition {
    return Not(condition)
}
