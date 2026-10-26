package lld.rippling.expenserules.rule

import lld.rippling.expenserules.entity.Expense
import lld.rippling.expenserules.entity.Severity
import lld.rippling.expenserules.entity.Violation
import java.time.YearMonth

enum class GroupBy(val keyOf: (Expense) -> String) {
    TRIP({ it.tripId }),
    EMPLOYEE({ it.employeeId }),
    DEPARTMENT({ it.department }),
    EMPLOYEE_MONTH({ "${it.employeeId}:${YearMonth.from(it.date)}" }),
}

enum class Field(val valueOf: (Expense) -> String) {
    EXPENSE_TYPE({ it.expenseType }),
    VENDOR_TYPE({ it.vendorType }),
    VENDOR_NAME({ it.vendorName.lowercase() }),
}

data class Trigger(
    val field: Field,
    val value: String,
)

interface ExpenseRule {
    val name: String
    val severity: Severity
    val trigger: Trigger?
        get() = null

    fun check(expense: Expense): Violation?
}

interface GroupRule {
    val name: String
    val severity: Severity
    val groupBy: GroupBy
    fun check(groupId: String, expenses: List<Expense>): Violation?
}
