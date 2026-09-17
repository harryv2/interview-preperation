package lld_self.splitwise

import lld_self.splitwise.entity.Expense
import lld_self.splitwise.entity.Money
import lld_self.splitwise.entity.Splitwise
import lld_self.splitwise.strategies.split.EqualSplitStrategy
import lld_self.splitwise.strategies.split.ExactSplitStrategy
import lld_self.splitwise.strategies.split.PercentageSplitStrategy
import kotlin.math.E
import kotlin.uuid.Uuid


fun main() {

    var app = Splitwise()

    var alice = app.createUser("alice")
    var bib = app.createUser("bob")

    var tom = app.createUser("tom")


    var goaTrip = app.createGroup(
        "Goa Trip",
        listOf<Uuid>(alice.id, bib.id, tom.id),
        false
    )

    goaTrip.addExpense(
        Expense.Regular(
            "Drinks",
            "Goa",
            Money.rupees(1000),
            paidBy = mapOf(
                alice to Money.rupees(1000)
            ),
            splitStrategy = EqualSplitStrategy(setOf(alice, bib, tom))
        )
    )

    goaTrip.addExpense(
        Expense.Regular(
            "boating",
            "Goa",
            Money.rupees(10000),
            paidBy = mapOf(
                alice to Money.rupees(10000)
            ),
            splitStrategy = PercentageSplitStrategy(
                mapOf(
                    bib to 40,
                    tom to 40,
                    alice to 20
                )
            )
        )
    )

    goaTrip.addExpense(
        Expense.Regular(
            "Kayaking",
            "Goa",
            Money.rupees(2000),
            paidBy = mapOf(
                tom to Money.rupees(2000)
            ),
            splitStrategy = ExactSplitStrategy(
                mapOf(
                    bib to Money.rupees(1000),
                    alice to Money.rupees(500),
                    tom to Money.rupees(500)
                )
            )
        )
    )

    println(goaTrip.getBalances())

    println(goaTrip.getTransfers())

    goaTrip.addExpense(
        Expense.Settlement(alice, Money.rupees(200), bib)
    )

    println(goaTrip.getBalances())

    println(goaTrip.getTransfers())

    app.getSummary(alice.id)
}