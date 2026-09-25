package lld.library.strategy

import lld.library.entity.Money
import java.time.LocalDate
import java.time.temporal.ChronoUnit


interface FinePolicy {
    fun fineFor(dueOn: LocalDate, returnedOn: LocalDate): Money
    fun borrowingBlockedAbove(): Money
}


class PerDayFine(
    private val perDay: Money,
    private val cap: Money,
    private val blockAbove: Money
) : FinePolicy {

    override fun fineFor(dueOn: LocalDate, returnedOn: LocalDate): Money {
        if (!returnedOn.isAfter(dueOn)) {
            return Money.ZERO
        }
        val daysLate = ChronoUnit.DAYS.between(dueOn, returnedOn)
        return minOf(perDay * daysLate, cap)
    }

    override fun borrowingBlockedAbove(): Money {
        return blockAbove
    }
}
