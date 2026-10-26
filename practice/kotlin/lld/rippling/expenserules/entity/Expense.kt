package lld.rippling.expenserules.entity

import java.math.BigDecimal
import java.time.LocalDate

data class Expense(
    val id: String,
    val tripId: String,
    val employeeId: String,
    val department: String,
    val amount: BigDecimal,
    val expenseType: String,
    val vendorType: String,
    val vendorName: String,
    val date: LocalDate,
)
