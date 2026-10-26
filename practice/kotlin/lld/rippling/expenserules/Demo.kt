package lld.rippling.expenserules

import lld.rippling.expenserules.entity.Expense
import lld.rippling.expenserules.entity.Severity
import lld.rippling.expenserules.followup.IncrementalRulesEngine
import lld.rippling.expenserules.followup.VersionedRuleBook
import lld.rippling.expenserules.rule.BannedExpenseTypeRule
import lld.rippling.expenserules.rule.GroupBy
import lld.rippling.expenserules.rule.GroupExpenseTypeLimitRule
import lld.rippling.expenserules.rule.GroupTotalLimitRule
import lld.rippling.expenserules.rule.MaxAmountRule
import lld.rippling.expenserules.rule.RuleFactory
import lld.rippling.expenserules.rule.VendorTypeLimitRule
import java.math.BigDecimal
import java.time.LocalDate

fun main() {
    val expenses = listOf(
        Expense("e1", "t1", "alice", "sales", BigDecimal("50"), "meals", "restaurant", "Pizza Place", LocalDate.parse("2026-01-10")),
        Expense("e2", "t1", "alice", "sales", BigDecimal("40"), "meals", "restaurant", "Cafe", LocalDate.parse("2026-01-11")),
        Expense("e3", "t1", "alice", "sales", BigDecimal("250"), "travel", "airline", "Air Co", LocalDate.parse("2026-01-12")),
        Expense("e4", "t2", "bob", "eng", BigDecimal("30"), "entertainment", "theatre", "Cinema", LocalDate.parse("2026-02-03")),
        Expense("e5", "t2", "bob", "eng", BigDecimal("20"), "meals", "cafe", "Coffee", LocalDate.parse("2026-02-04")),
        Expense("e6", "t3", "bob", "eng", BigDecimal("120"), "meals", "bar", "Lucky Bar", LocalDate.parse("2026-02-20")),
    )

    println("-- base: per expense + per trip rules, every violation")
    val base = RulesEngine(
        expenseRules = listOf(
            MaxAmountRule(BigDecimal("175")),
            VendorTypeLimitRule("restaurant", BigDecimal("45")),
            BannedExpenseTypeRule("entertainment"),
        ),
        groupRules = listOf(
            GroupTotalLimitRule(GroupBy.TRIP, BigDecimal("300")),
            GroupExpenseTypeLimitRule(GroupBy.TRIP, "meals", BigDecimal("80")),
        ),
    )
    printReport(base.evaluate(expenses))

    println("-- follow-ups: rules from config, composite, employee / department groups, severity")
    val fromConfig = RulesEngine(
        RuleFactory.fromConfig(
            listOf(
                mapOf("type" to "max_amount", "limit" to "175"),
                mapOf("type" to "vendor_type_limit", "vendorType" to "restaurant", "limit" to "45", "severity" to "warn"),
                mapOf(
                    "type" to "all_of",
                    "name" to "big_bar_tab",
                    "rules" to listOf(
                        mapOf("type" to "vendor_type_limit", "vendorType" to "bar", "limit" to "100"),
                        mapOf("type" to "banned_vendor", "vendorName" to "Lucky Bar"),
                    ),
                ),
                mapOf(
                    "type" to "any_of",
                    "name" to "no_fun",
                    "severity" to "warn",
                    "rules" to listOf(
                        mapOf("type" to "banned_expense_type", "expenseType" to "entertainment"),
                        mapOf("type" to "banned_expense_type", "expenseType" to "gifts"),
                    ),
                ),
                mapOf("type" to "total_limit", "groupBy" to "employee", "limit" to "300"),
                mapOf("type" to "expense_type_limit", "groupBy" to "department", "expenseType" to "meals", "limit" to "100", "severity" to "warn"),
            )
        )
    )
    val report = fromConfig.evaluate(expenses)
    printReport(report)
    println("  blocked=${report.blocked}")
    println("  e6 only: ${report.forTarget("e6").map { it.ruleName }}")

    println("-- bad config fails on load, not on the first expense")
    runCatching { RuleFactory.fromConfig(listOf(mapOf("type" to "max_amonut", "limit" to "1"))) }
        .onFailure { println("  ${it.message}") }

    println("-- monthly group: per employee per month")
    val monthly = RulesEngine(
        RuleFactory.fromConfig(listOf(mapOf("type" to "total_limit", "groupBy" to "employee_month", "limit" to "150")))
    )
    printReport(monthly.evaluate(expenses))

    println("-- versioning: restaurant limit 45 in January, 60 from February")
    val book = VersionedRuleBook()
    book.publish(
        LocalDate.parse("2026-01-01"),
        RuleFactory.fromConfig(listOf(mapOf("type" to "vendor_type_limit", "vendorType" to "restaurant", "limit" to "45"))),
    )
    book.publish(
        LocalDate.parse("2026-02-01"),
        RuleFactory.fromConfig(listOf(mapOf("type" to "vendor_type_limit", "vendorType" to "restaurant", "limit" to "60"))),
    )
    val lateRestaurant = Expense("e7", "t4", "carol", "hr", BigDecimal("50"), "meals", "restaurant", "Diner", LocalDate.parse("2026-02-15"))
    printReport(book.evaluate(expenses + lateRestaurant))
    println("  e1 checked against ${book.versionOn(expenses[0].date)}, e7 against ${book.versionOn(lateRestaurant.date)}")

    println("-- incremental: expenses arrive one at a time, only their groups are re-checked")
    val live = IncrementalRulesEngine(
        RuleFactory.fromConfig(
            listOf(
                mapOf("type" to "max_amount", "limit" to "175"),
                mapOf("type" to "total_limit", "groupBy" to "trip", "limit" to "300"),
            )
        )
    )
    for (expense in expenses) {
        val raised = live.add(expense)
        println("  add ${expense.id} -> ${raised.map { "${it.targetId} ${it.ruleName}" }}")
    }
    val extra = Expense("e8", "t1", "alice", "sales", BigDecimal("10"), "meals", "cafe", "Coffee", LocalDate.parse("2026-01-13"))
    println("  add e8 (t1 already over) -> ${live.add(extra).map { it.ruleName }}")
    println("  open: ${live.openViolations.map { "${it.targetId} ${it.ruleName}" }}")
}

private fun printReport(report: Report) {
    for (violation in report.violations) {
        val tag = if (violation.severity == Severity.WARN) "warn " else "block"
        println("  [$tag] ${violation.targetId} ${violation.ruleName}: ${violation.message}")
    }
}
