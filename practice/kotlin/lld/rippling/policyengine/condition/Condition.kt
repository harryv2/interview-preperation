package lld.rippling.policyengine.condition

typealias Facts = Map<String, Any>

interface Condition {
    fun test(facts: Facts): Boolean
    fun describe(): String
}

enum class Op(val symbol: String) {
    EQ("=="), NEQ("!="), GT(">"), GTE(">="), LT("<"), LTE("<="), IN("in")
}

class FieldCondition(
    private val field: String,
    private val op: Op,
    private val value: Any,
) : Condition {

    override fun test(facts: Facts): Boolean {
        val actual = facts[field] ?: return false
        return when (op) {
            Op.EQ -> same(actual, value)
            Op.NEQ -> !same(actual, value)
            Op.GT -> number(actual) > number(value)
            Op.GTE -> number(actual) >= number(value)
            Op.LT -> number(actual) < number(value)
            Op.LTE -> number(actual) <= number(value)
            Op.IN -> (value as Collection<*>).any { it != null && same(actual, it) }
        }
    }

    override fun describe(): String {
        return "$field ${op.symbol} $value"
    }

    private fun same(a: Any, b: Any): Boolean {
        if (a is Number && b is Number) {
            return a.toDouble() == b.toDouble()
        }
        return a.toString().equals(b.toString(), ignoreCase = true)
    }

    private fun number(v: Any): Double {
        return (v as? Number)?.toDouble() ?: v.toString().toDouble()
    }
}

class AndCondition(private val children: List<Condition>) : Condition {
    override fun test(facts: Facts): Boolean {
        return children.all { it.test(facts) }
    }

    override fun describe(): String {
        return children.joinToString(" AND ", "(", ")") { it.describe() }
    }
}

class OrCondition(private val children: List<Condition>) : Condition {
    override fun test(facts: Facts): Boolean {
        return children.any { it.test(facts) }
    }

    override fun describe(): String {
        return children.joinToString(" OR ", "(", ")") { it.describe() }
    }
}

class NotCondition(private val child: Condition) : Condition {
    override fun test(facts: Facts): Boolean {
        return !child.test(facts)
    }

    override fun describe(): String {
        return "NOT ${child.describe()}"
    }
}
