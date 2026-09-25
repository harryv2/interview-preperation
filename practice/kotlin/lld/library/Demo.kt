package lld.library

import lld.library.entity.Book
import lld.library.entity.BookCopy
import lld.library.entity.Member
import lld.library.entity.Money
import lld.library.entity.Tier
import lld.library.service.LibraryService
import lld.library.strategy.PerDayFine
import java.time.LocalDate

fun main() {

    val dune = Book("978-0441", "Dune", "Frank Herbert")
    val sapiens = Book("978-0062", "Sapiens", "Yuval Noah Harari")

    val copies = listOf(
        BookCopy("B1", dune),
        BookCopy("B2", dune),
        BookCopy("B3", sapiens),
        BookCopy("B4", sapiens),
        BookCopy("B5", sapiens)
    )

    val asha = Member("M1", "Asha", Tier.BASIC)
    val ravi = Member("M2", "Ravi", Tier.BASIC)
    val kim = Member("M3", "Kim", Tier.PREMIUM)

    val service = LibraryService(
        copies = copies,
        members = listOf(asha, ravi, kim),
        finePolicy = PerDayFine(
            perDay = Money.rupees(10),
            cap = Money.rupees(500),
            blockAbove = Money.rupees(100)
        )
    )

    val day1 = LocalDate.of(2026, 10, 1)

    banner("search")
    println("  'dune' -> ${service.search("dune")}")
    println("  'harari' -> ${service.search("harari")}")

    banner("borrow both copies of Dune")
    val ashaLoan = service.borrow(asha.id, dune.isbn, day1)
    println("  $ashaLoan")
    val raviLoan = service.borrow(ravi.id, dune.isbn, day1)
    println("  $raviLoan")
    println("  available copies of Dune: ${service.availableCopies(dune.isbn).size}")

    banner("nothing left, so Kim holds")
    attempt("Kim borrows Dune") {
        service.borrow(kim.id, dune.isbn, day1)
    }
    println("  Kim is number ${service.placeHold(kim.id, dune.isbn)} in the queue")
    attempt("Kim holds twice") {
        service.placeHold(kim.id, dune.isbn)
    }

    banner("Asha returns very late, the copy goes to Kim")
    val returned = service.returnCopy("B1", day1.plusDays(35))
    println("  $returned")
    println("  Asha now $asha")
    println("  copy B1 is ${copies[0]}")
    attempt("Ravi takes the held copy") {
        service.borrow(ravi.id, dune.isbn, day1.plusDays(35))
    }
    println("  ${service.borrow(kim.id, dune.isbn, day1.plusDays(35))}")

    banner("fines block borrowing")
    attempt("Asha borrows while owing ${asha.outstandingFine}") {
        service.borrow(asha.id, sapiens.isbn, day1.plusDays(35))
    }
    println("  Asha pays Rs 150, still owes ${service.payFine(asha.id, Money.rupees(150))}")
    println("  ${service.borrow(asha.id, sapiens.isbn, day1.plusDays(35))}")

    banner("loan limit")
    service.borrow(ravi.id, sapiens.isbn, day1)
    service.borrow(ravi.id, sapiens.isbn, day1)
    println("  Ravi now holds ${service.openLoansOf(ravi.id).size} of ${ravi.tier.loanLimit}")
    attempt("Ravi takes a fourth book") {
        service.borrow(ravi.id, sapiens.isbn, day1)
    }

    banner("rejected")
    attempt("return a barcode that does not exist") {
        service.returnCopy("B9", day1)
    }
    attempt("unknown member") {
        service.borrow("M9", dune.isbn, day1)
    }
    attempt("unknown isbn") {
        service.borrow(asha.id, "000", day1)
    }
}

private fun banner(title: String) {
    println("\n== $title ==")
}

private fun attempt(label: String, action: () -> Unit) {
    try {
        action()
        println("  $label -> allowed")
    } catch (e: RuntimeException) {
        println("  $label -> rejected: ${e.message}")
    }
}
