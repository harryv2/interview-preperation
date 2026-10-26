package lld.rippling.policyengine

import lld.rippling.policyengine.condition.Condition
import lld.rippling.policyengine.condition.ConditionParser
import lld.rippling.policyengine.condition.Facts

enum class Action { ALLOW, REQUIRE_MANAGER_APPROVAL, REQUIRE_VP_APPROVAL, REJECT }

class Policy(
    val name: String,
    val condition: Condition,
    val action: Action,
)

data class Decision(
    val action: Action,
    val matched: List<Policy>,
)

class PolicyEngine {
    private val policies = ArrayList<Policy>()

    fun add(policy: Policy) {
        policies += policy
    }

    fun addFromConfig(name: String, action: String, spec: Map<String, Any>) {
        policies += Policy(name, ConditionParser.parse(spec), Action.valueOf(action))
    }

    fun evaluate(facts: Facts): Decision {
        val matched = policies.filter { it.condition.test(facts) }
        val strongest = matched.maxOfOrNull { it.action } ?: Action.ALLOW
        return Decision(strongest, matched)
    }
}
