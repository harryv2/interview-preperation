package lld.ruleengine.strategies.conditions

enum class Operator {
    GT,
    LT,
    EQ;

    fun test(actual: Any, expected: Any): Boolean {
        return when (this) {
            GT -> compare(actual, expected) > 0
            LT -> compare(actual, expected) < 0
            EQ -> if (actual is Number && expected is Number) compare(actual, expected) == 0 else actual == expected
        }
    }

    private fun compare(actual: Any, expected: Any): Int {
        require(actual is Number && expected is Number) { "$this needs numbers, got $actual and $expected" }
        return actual.toDouble().compareTo(expected.toDouble())
    }
}
