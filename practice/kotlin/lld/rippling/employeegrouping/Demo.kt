package lld.rippling.employeegrouping

import lld.rippling.employeegrouping.entity.Employee

fun main() {
    val report = EmployeeReport(
        listOf(
            Employee("1", "Asha", "eng", "NYC", "SWE", 150_000.0, 29),
            Employee("2", "Ben", "eng", "SF", "SWE", 170_000.0, 34),
            Employee("3", "Chen", "eng", "SF", "Manager", 210_000.0, 41),
            Employee("4", "Dev", "sales", "NYC", "AE", 120_000.0, 31),
            Employee("5", "Eve", "sales", "NYC", "Manager", 160_000.0, 45),
            Employee("6", "Fay", "hr", "SF", "HRBP", 110_000.0, 38),
        )
    )

    println("-- total salary by department")
    println("  ${report.groupBy({ it.department }, SumAggregator { it.salary })}")

    println("-- average age by location")
    println("  ${report.groupBy({ it.location }, AverageAggregator { it.age.toDouble() })}")

    println("-- headcount by (department, location)")
    println("  ${report.groupBy({ it.department to it.location }, CountAggregator())}")

    println("-- max salary by title, only SF")
    println("  ${report.groupBy({ it.title }, MaxAggregator { it.salary }) { it.location == "SF" }}")

    println("-- filterBy age > 35")
    println("  ${report.filterBy { it.age > 35 }.map { it.name }}")

    println("-- follow-up: several aggregates per group")
    val stats = report.groupByAll(
        { it.department },
        listOf(
            CountAggregator(),
            SumAggregator("total_salary") { it.salary },
            AverageAggregator("avg_salary") { it.salary },
            AverageAggregator("avg_age") { it.age.toDouble() },
        ),
    )
    for ((department, values) in stats) {
        println("  $department $values")
    }

    println("-- follow-up: nested department -> location -> headcount")
    println("  ${report.nested(listOf({ it.department }, { it.location }), CountAggregator())}")

    println("-- follow-up: top 2 departments by total salary")
    println("  ${report.top({ it.department }, SumAggregator { it.salary }, 2)}")

    println("-- follow-up: raw CSV rows, columns chosen by name at runtime")
    val rows = Rows.parseCsv(
        """
        name,department,location,salary
        Asha,eng,NYC,150000
        Ben,eng,SF,170000
        Dev,sales,NYC,120000
        Eve,sales,NYC,160000
        """
    )
    val csv = Report(rows)
    println("  ${csv.groupBy(Rows.column("department"), Rows.aggregator("avg", "salary"))}")
    println("  ${csv.groupBy(Rows.columns(listOf("department", "location")), Rows.aggregator("count"))}")
    println("  ${csv.groupBy(Rows.column("location"), Rows.aggregator("max", "salary")) { it["department"] == "sales" }}")

    println("-- bad input fails with a clear message")
    runCatching { csv.groupBy(Rows.column("team"), Rows.aggregator("count")) }.onFailure { println("  ${it.message}") }
    runCatching { Rows.aggregator("median", "salary") }.onFailure { println("  ${it.message}") }
}
