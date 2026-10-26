package lld.rippling.expenserules.rule

import lld.rippling.expenserules.entity.Expense
import lld.rippling.expenserules.entity.Severity
import lld.rippling.expenserules.entity.Violation
import java.math.BigDecimal

class MaxAmountRule(
    private val limit: BigDecimal,
    override val severity: Severity = Severity.BLOCK,
) : ExpenseRule {
    override val name = "max_amount"

    override fun check(expense: Expense): Violation? {
        if (expense.amount <= limit) {
            return null
        }
        return Violation(name, expense.id, "${expense.amount} is over $limit", severity)
    }
}

class VendorTypeLimitRule(
    private val vendorType: String,
    private val limit: BigDecimal,
    override val severity: Severity = Severity.BLOCK,
) : ExpenseRule {
    override val name = "vendor_type_limit"
    override val trigger = Trigger(Field.VENDOR_TYPE, vendorType)

    override fun check(expense: Expense): Violation? {
        if (expense.vendorType != vendorType || expense.amount <= limit) {
            return null
        }
        return Violation(name, expense.id, "$vendorType expense ${expense.amount} is over $limit", severity)
    }
}

class BannedExpenseTypeRule(
    private val expenseType: String,
    override val severity: Severity = Severity.BLOCK,
) : ExpenseRule {
    override val name = "banned_expense_type"
    override val trigger = Trigger(Field.EXPENSE_TYPE, expenseType)

    override fun check(expense: Expense): Violation? {
        if (expense.expenseType != expenseType) {
            return null
        }
        return Violation(name, expense.id, "$expenseType is not allowed", severity)
    }
}

class BannedVendorRule(
    private val vendorName: String,
    override val severity: Severity = Severity.BLOCK,
) : ExpenseRule {
    override val name = "banned_vendor"
    override val trigger = Trigger(Field.VENDOR_NAME, vendorName.lowercase())

    override fun check(expense: Expense): Violation? {
        if (!expense.vendorName.equals(vendorName, ignoreCase = true)) {
            return null
        }
        return Violation(name, expense.id, "$vendorName is not allowed", severity)
    }
}
