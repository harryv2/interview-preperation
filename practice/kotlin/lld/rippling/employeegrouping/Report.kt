package lld.rippling.employeegrouping

import lld.rippling.employeegrouping.entity.Employee

typealias EmployeeReport = Report<Employee>

class Report<T>(private val items: List<T>) {

    fun <K> groupBy(
        key: (T) -> K,
        aggregator: Aggregator<T>,
        filter: (T) -> Boolean = { true },
    ): Map<K, Double> {
        return items
            .filter(filter)
            .groupBy(key)
            .mapValues { (_, group) -> aggregator.aggregate(group) }
    }

    fun <K> groupByAll(
        key: (T) -> K,
        aggregators: List<Aggregator<T>>,
        filter: (T) -> Boolean = { true },
    ): Map<K, Map<String, Double>> {
        val names = aggregators.map { it.name }
        require(names.size == names.toSet().size) { "Aggregator names must be unique: $names" }
        return items
            .filter(filter)
            .groupBy(key)
            .mapValues { (_, group) ->
                aggregators.associate { it.name to it.aggregate(group) }
            }
    }

    fun nested(
        keys: List<(T) -> Any>,
        aggregator: Aggregator<T>,
        filter: (T) -> Boolean = { true },
    ): Map<Any, Any> {
        require(keys.isNotEmpty()) { "Need at least one key" }
        return nest(items.filter(filter), keys, aggregator)
    }

    fun <K> top(
        key: (T) -> K,
        aggregator: Aggregator<T>,
        k: Int,
        descending: Boolean = true,
    ): List<Pair<K, Double>> {
        val totals = groupBy(key, aggregator).toList()
        val sorted = if (descending) totals.sortedByDescending { it.second } else totals.sortedBy { it.second }
        return sorted.take(k)
    }

    fun filterBy(filter: (T) -> Boolean): List<T> {
        return items.filter(filter)
    }

    private fun nest(group: List<T>, keys: List<(T) -> Any>, aggregator: Aggregator<T>): Map<Any, Any> {
        val first = keys.first()
        val rest = keys.drop(1)
        return group.groupBy(first).mapValues { (_, sub) ->
            if (rest.isEmpty()) {
                aggregator.aggregate(sub)
            } else {
                nest(sub, rest, aggregator)
            }
        }
    }
}
