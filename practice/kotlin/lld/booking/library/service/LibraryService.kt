package lld.booking.library.service

import lld.booking.library.entity.Book
import lld.booking.library.entity.BookCopy
import lld.booking.library.entity.Hold
import lld.booking.library.entity.Loan
import lld.booking.library.entity.Member
import lld.booking.library.entity.Money
import lld.booking.library.strategy.FinePolicy
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class LibraryService(
    copies: List<BookCopy>,
    members: List<Member>,
    private val finePolicy: FinePolicy
) {

    private val lock = ReentrantLock()
    private val copiesByBarcode = copies.associateBy { it.barcode }
    private val copiesByIsbn = copies.groupBy { it.book.isbn }
    private val membersById = members.associateBy { it.id }
    private val loans = HashMap<String, Loan>()
    private val holds = LinkedHashMap<String, ArrayDeque<Hold>>()

    fun search(text: String): List<Book> {
        val needle = text.lowercase()
        return copiesByIsbn.values
            .map { it.first().book }
            .filter {
                it.title.lowercase().contains(needle) ||
                    it.author.lowercase().contains(needle) ||
                    it.isbn == text
            }
    }

    fun availableCopies(isbn: String): List<BookCopy> {
        lock.withLock {
            return copiesByIsbn[isbn].orEmpty().filter { it.status == lld.booking.library.entity.CopyStatus.AVAILABLE }
        }
    }

    fun borrow(memberId: String, isbn: String, on: LocalDate = LocalDate.now()): Loan {
        lock.withLock {
            val member = member(memberId)
            require(member.outstandingFine <= finePolicy.borrowingBlockedAbove()) {
                "${member.name} owes ${member.outstandingFine}, clear it before borrowing"
            }
            require(openLoansOf(memberId).size < member.tier.loanLimit) {
                "${member.name} already holds ${member.tier.loanLimit} books"
            }

            val copies = copiesByIsbn[isbn]
            require(!copies.isNullOrEmpty()) { "No book with isbn $isbn" }

            val copy = copies.firstOrNull { it.isAvailableTo(memberId) }
            requireNotNull(copy) { "Every copy of ${copies.first().book} is out, place a hold" }

            copy.loanOut()
            dropHold(memberId, isbn)

            val loan = Loan(
                id = UUID.randomUUID().toString().take(8),
                memberId = memberId,
                copy = copy,
                borrowedOn = on,
                dueOn = on.plusDays(member.tier.loanDays)
            )
            loans[loan.id] = loan
            return loan
        }
    }

    fun returnCopy(barcode: String, on: LocalDate = LocalDate.now()): Loan {
        lock.withLock {
            val copy = copiesByBarcode[barcode]
            requireNotNull(copy) { "No copy $barcode" }

            val loan = loans.values.firstOrNull { it.copy.barcode == barcode && it.isOpen() }
            requireNotNull(loan) { "Copy $barcode is not on loan" }

            val fine = finePolicy.fineFor(loan.dueOn, on)
            loan.close(on, fine)
            if (fine > Money.ZERO) {
                member(loan.memberId).addFine(fine)
            }

            val nextHolder = holds[copy.book.isbn]?.removeFirstOrNull()
            if (nextHolder == null) {
                copy.shelve()
            } else {
                copy.holdFor(nextHolder.memberId)
            }
            return loan
        }
    }

    fun placeHold(memberId: String, isbn: String): Int {
        lock.withLock {
            member(memberId)
            require(copiesByIsbn.containsKey(isbn)) { "No book with isbn $isbn" }

            val queue = holds.getOrPut(isbn) { ArrayDeque() }
            require(queue.none { it.memberId == memberId }) { "$memberId already holds a place in that queue" }

            queue.addLast(Hold(memberId, isbn))
            return queue.size
        }
    }

    fun payFine(memberId: String, amount: Money): Money {
        lock.withLock {
            val member = member(memberId)
            member.payFine(amount)
            return member.outstandingFine
        }
    }

    fun openLoansOf(memberId: String): List<Loan> {
        return loans.values.filter { it.memberId == memberId && it.isOpen() }
    }

    fun loansOf(memberId: String): List<Loan> {
        return loans.values.filter { it.memberId == memberId }
    }

    private fun dropHold(memberId: String, isbn: String) {
        holds[isbn]?.removeIf { it.memberId == memberId }
    }

    private fun member(memberId: String): Member {
        val member = membersById[memberId]
        requireNotNull(member) { "No member $memberId" }
        return member
    }
}
