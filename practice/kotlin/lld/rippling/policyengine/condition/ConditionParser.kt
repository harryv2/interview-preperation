package lld.rippling.policyengine.condition

object ConditionParser {

    fun parse(spec: Map<String, Any>): Condition {
        spec["and"]?.let { return AndCondition(children(it)) }
        spec["or"]?.let { return OrCondition(children(it)) }
        spec["not"]?.let { return NotCondition(parse(asMap(it))) }

        val field = requireNotNull(spec["field"]) { "Missing field in $spec" }.toString()
        val op = Op.valueOf(requireNotNull(spec["op"]) { "Missing op in $spec" }.toString().uppercase())
        val value = requireNotNull(spec["value"]) { "Missing value in $spec" }
        return FieldCondition(field, op, value)
    }

    private fun children(node: Any): List<Condition> {
        val list = node as? List<*> ?: throw IllegalArgumentException("Expected a list, got $node")
        require(list.isNotEmpty()) { "Empty group" }
        return list.map { parse(asMap(it!!)) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun asMap(node: Any): Map<String, Any> {
        return node as? Map<String, Any> ?: throw IllegalArgumentException("Expected an object, got $node")
    }
}
