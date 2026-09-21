package lld.ruleengine.entity

class Facts(private val values: Map<String, Any>) {

    operator fun get(key: String): Any? {
        return values[key]
    }

    fun number(key: String): Double? {
        return (values[key] as? Number)?.toDouble()
    }

    override fun toString(): String {
        return values.toString()
    }
}
