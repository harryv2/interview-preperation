package lld.rippling.employeegrouping

interface Aggregator<T> {
    val name: String
    fun aggregate(items: List<T>): Double
}

class SumAggregator<T>(
    override val name: String = "sum",
    private val field: (T) -> Double,
) : Aggregator<T> {

    override fun aggregate(items: List<T>): Double {
        return items.sumOf(field)
    }
}

class AverageAggregator<T>(
    override val name: String = "avg",
    private val field: (T) -> Double,
) : Aggregator<T> {

    override fun aggregate(items: List<T>): Double {
        if (items.isEmpty()) {
            return 0.0
        }
        return items.sumOf(field) / items.size
    }
}

class MaxAggregator<T>(
    override val name: String = "max",
    private val field: (T) -> Double,
) : Aggregator<T> {

    override fun aggregate(items: List<T>): Double {
        return items.maxOfOrNull(field) ?: 0.0
    }
}

class MinAggregator<T>(
    override val name: String = "min",
    private val field: (T) -> Double,
) : Aggregator<T> {

    override fun aggregate(items: List<T>): Double {
        return items.minOfOrNull(field) ?: 0.0
    }
}

class CountAggregator<T> : Aggregator<T> {
    override val name = "count"

    override fun aggregate(items: List<T>): Double {
        return items.size.toDouble()
    }
}
