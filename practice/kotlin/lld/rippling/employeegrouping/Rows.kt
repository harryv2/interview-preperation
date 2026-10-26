package lld.rippling.employeegrouping

typealias Row = Map<String, String>

object Rows {

    fun parseCsv(text: String): List<Row> {
        val lines = text.trim().lines().filter { it.isNotBlank() }
        require(lines.isNotEmpty()) { "Empty CSV" }
        val header = lines.first().split(",").map { it.trim() }
        return lines.drop(1).mapIndexed { index, line ->
            val cells = line.split(",").map { it.trim() }
            require(cells.size == header.size) { "Row ${index + 1} has ${cells.size} cells, header has ${header.size}" }
            header.zip(cells).toMap()
        }
    }

    fun column(name: String): (Row) -> String {
        return { row -> requireNotNull(row[name]) { "No column $name" } }
    }

    fun number(name: String): (Row) -> Double {
        return { row ->
            val raw = requireNotNull(row[name]) { "No column $name" }
            requireNotNull(raw.toDoubleOrNull()) { "Column $name is not a number: $raw" }
        }
    }

    fun columns(names: List<String>): (Row) -> List<String> {
        return { row -> names.map { column(it)(row) } }
    }

    fun aggregator(function: String, field: String? = null): Aggregator<Row> {
        if (function == "count") {
            return CountAggregator()
        }
        val selector = number(requireNotNull(field) { "$function needs a field" })
        return when (function) {
            "sum" -> SumAggregator("sum_$field", selector)
            "avg" -> AverageAggregator("avg_$field", selector)
            "max" -> MaxAggregator("max_$field", selector)
            "min" -> MinAggregator("min_$field", selector)
            else -> throw IllegalArgumentException("Unknown aggregate $function")
        }
    }
}
