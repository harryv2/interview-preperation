package lld.rippling.policyengine

import lld.rippling.policyengine.condition.AndCondition
import lld.rippling.policyengine.condition.FieldCondition
import lld.rippling.policyengine.condition.Op

fun main() {
    val engine = PolicyEngine()

    engine.add(
        Policy(
            "marketing over 500",
            AndCondition(
                listOf(
                    FieldCondition("amount", Op.GT, 500),
                    FieldCondition("department", Op.EQ, "Marketing"),
                )
            ),
            Action.REQUIRE_VP_APPROVAL,
        )
    )

    engine.addFromConfig(
        name = "big spend outside eng, unless travel",
        action = "REQUIRE_MANAGER_APPROVAL",
        spec = mapOf(
            "and" to listOf(
                mapOf("field" to "amount", "op" to "gte", "value" to 200),
                mapOf("not" to mapOf("field" to "department", "op" to "eq", "value" to "Engineering")),
                mapOf(
                    "or" to listOf(
                        mapOf("field" to "category", "op" to "neq", "value" to "travel"),
                        mapOf("field" to "amount", "op" to "gt", "value" to 1000),
                    )
                ),
            )
        ),
    )

    engine.addFromConfig(
        name = "blocked categories",
        action = "REJECT",
        spec = mapOf("field" to "category", "op" to "in", "value" to listOf("gambling", "alcohol")),
    )

    val expenses = listOf(
        mapOf("amount" to 800, "department" to "Marketing", "category" to "ads"),
        mapOf("amount" to 300, "department" to "Sales", "category" to "meals"),
        mapOf("amount" to 300, "department" to "Sales", "category" to "travel"),
        mapOf("amount" to 300, "department" to "Engineering", "category" to "meals"),
        mapOf("amount" to 50, "department" to "Sales", "category" to "alcohol"),
    )

    for (facts in expenses) {
        val decision = engine.evaluate(facts)
        println("-- $facts")
        println("  ${decision.action} via ${decision.matched.map { it.name }}")
    }

    println("-- the config policy, printed back")
    println("  ${engine.evaluate(expenses[1]).matched.first().condition.describe()}")
}
