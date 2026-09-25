package lld.fintech.splitwise

import lld.fintech.splitwise.entity.Expenses
import lld.fintech.splitwise.entity.Money
import lld.fintech.splitwise.entity.User
import lld.fintech.splitwise.service.Group
import lld.fintech.splitwise.service.SplitwiseService
import kotlin.math.absoluteValue

// =====================================================================
//  DEMO
// =====================================================================

fun main() {
    val alice = User("u1", "Alice", "alice@mail.com")
    val bob = User("u2", "Bob", "bob@mail.com")
    val carol = User("u3", "Carol", "carol@mail.com")
    val everyone = listOf(alice, bob, carol)

    val app = SplitwiseService()
    val goa = app.createGroup("Goa Trip", everyone)

    // 1. One payer, equal three ways.
    val hotel = goa.addExpense(
        Expenses.equal(
            title = "Hotel",
            total = Money.rupees(3000),
            paidBy = mapOf(alice to Money.rupees(3000)),
            participants = everyone,
            id = "exp-hotel"
        )
    )

    // 2. TWO payers on one bill. No special case needed.
    goa.addExpense(
        Expenses.equal(
            title = "Dinner",
            total = Money.rupees(1200),
            paidBy = mapOf(alice to Money.rupees(800), bob to Money.rupees(400)),
            participants = everyone,
            id = "exp-dinner"
        )
    )

    // 3. 100000 paise / 3 leaves one paisa over. Watch where it lands.
    goa.addExpense(
        Expenses.equal(
            title = "Taxi",
            total = Money.rupees(1000),
            paidBy = mapOf(carol to Money.rupees(1000)),
            participants = everyone,
            id = "exp-taxi"
        )
    )

    // 4. Percentage split.
    goa.addExpense(
        Expenses.percent(
            title = "Scuba (Carol sat out)",
            total = Money.rupees(2000),
            paidBy = mapOf(bob to Money.rupees(2000)),
            percentages = mapOf(alice to 50.0, bob to 50.0),
            id = "exp-scuba"
        )
    )

    report("After four expenses", goa)

    // 5. Edit. The strategy is stored, so the split rule re-runs itself.
    // paidBy has to move with the total, or the "payments add up" check fails.
    goa.updateExpense(
        hotel.edited(
            totalAmount = Money.rupees(2400),
            paidBy = mapOf(alice to Money.rupees(2400))
        )
    )
    report("After editing Hotel from 3000 to 2400", goa)

    // 6. Delete.
    goa.deleteExpense("exp-scuba")
    report("After deleting Scuba", goa)

    // 7. Settlement. Just another expense.
    app.settleUp(goa, from = carol, to = alice, amount = Money.rupees(500))
    report("After Carol pays Alice 500", goa)

    // 8. A one-off expense outside any real group.
    val pair = app.oneOnOne(alice, bob)
    pair.addExpense(
        Expenses.exact(
            title = "Movie tickets",
            total = Money.rupees(600),
            paidBy = mapOf(bob to Money.rupees(600)),
            owed = mapOf(alice to Money.rupees(400), bob to Money.rupees(200)),
            id = "exp-movie"
        )
    )

    // 9. A group created simplified. Three debts arranged in a circle:
    //    everyone nets to zero, so the simplified plan is empty while the
    //    raw pairwise view still lists all three hops.
    val flat = app.createGroup("Flatmates", everyone, simplified = true)
    flat.addExpense(
        Expenses.equal(
            title = "Rent",
            total = Money.rupees(900),
            paidBy = mapOf(alice to Money.rupees(900)),
            participants = listOf(alice, bob),
            id = "exp-rent"
        )
    )
    flat.addExpense(
        Expenses.equal(
            title = "Wifi",
            total = Money.rupees(900),
            paidBy = mapOf(bob to Money.rupees(900)),
            participants = listOf(bob, carol),
            id = "exp-wifi"
        )
    )
    flat.addExpense(
        Expenses.equal(
            title = "Groceries",
            total = Money.rupees(900),
            paidBy = mapOf(carol to Money.rupees(900)),
            participants = listOf(carol, alice),
            id = "exp-groceries"
        )
    )
    report("Flatmates, created simplified", flat)

    println("\n=== Alice across every group ===")
    val summary = app.summaryFor(alice)
    summary.perGroup.forEach { (group, balance) -> println("  $group: $balance") }
    println("  owed ${summary.totalOwed}, owes ${summary.totalOwing}, net ${summary.net}")
}

private fun report(label: String, group: Group) {
    println("\n=== $label ===")

    println("balances:")
    group.balances().forEach { (user, balance) ->
        val verb = if (balance.paise > 0) "is owed" else "owes"
        println("  ${user.name.padEnd(6)} $verb ${Money(balance.paise.absoluteValue)}")
    }

    // Both views are shown for every group, so the flag is restored rather
    // than forced back to false: the group may have been created simplified.
    val original = group.isSimplified

    group.isSimplified = false
    println("raw pairwise:")
    group.settlementPlan().forEach { println("  $it") }

    group.isSimplified = true
    println("simplified:")
    group.settlementPlan().forEach { println("  $it") }

    group.isSimplified = original
}
