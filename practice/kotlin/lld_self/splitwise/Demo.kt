package lld_self.splitwise

import lld_self.splitwise.entity.Expense
import lld_self.splitwise.entity.Money
import lld_self.splitwise.entity.Splitwise
import lld_self.splitwise.strategies.split.EqualSplitStrategy
import lld_self.splitwise.strategies.split.ExactSplitStrategy
import lld_self.splitwise.strategies.split.PercentageSplitStrategy
import kotlin.uuid.Uuid


fun main() {

    val app = Splitwise()

    val alice = app.createUser("alice")
    val bob = app.createUser("bob")

    val tom = app.createUser("tom")


    val goaTrip = app.createGroup(
        "Goa Trip",
        listOf<Uuid>(alice.id, bob.id, tom.id),
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
            splitStrategy = EqualSplitStrategy(setOf(alice, bob, tom))
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
                    bob to 40,
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
                    bob to Money.rupees(1000),
                    alice to Money.rupees(500),
                    tom to Money.rupees(500)
                )
            )
        )
    )

    println(goaTrip.getBalances())

    println(goaTrip.getTransfers())

    goaTrip.addExpense(
        Expense.Settlement(alice, Money.rupees(200), bob)
    )

    println(goaTrip.getBalances())

    println(goaTrip.getTransfers())

    app.getSummary(alice.id)
}