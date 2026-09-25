package lld.booking.library.entity

import java.time.LocalDate


class Loan(
    val id: String,
    val memberId: String,
    val copy: BookCopy,
    val borrowedOn: LocalDate,
    val dueOn: LocalDate
) {

    var returnedOn: LocalDate? = null
        private set

    var fine: Money = Money.ZERO
        private set

    fun isOpen(): Boolean {
        return returnedOn == null
    }

    fun close(on: LocalDate, charged: Money) {
        check(isOpen()) { "Loan $id is already closed" }
        returnedOn = on
        fine = charged
    }

    override fun toString(): String {
        val state = returnedOn?.let { "returned $it, fine $fine" } ?: "due $dueOn"
        return "Loan $id ${copy.book} -> $memberId ($state)"
    }
}


data class Hold(
    val memberId: String,
    val isbn: String
)
