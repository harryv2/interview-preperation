package lld.fintech.splitwise.service

import lld.fintech.splitwise.entity.Expense
import lld.fintech.splitwise.entity.Expenses
import lld.fintech.splitwise.entity.Money
import lld.fintech.splitwise.entity.User

// =====================================================================
//  SERVICE
//
//  Holds the groups and answers cross-group questions, which is what
//  the home screen needs ("you are owed 1600 overall").
//
//  A one-off expense between two people is an ad hoc two-person group,
//  so there is exactly one container type in the whole system.
// =====================================================================

class SplitwiseService {

    private val groups = LinkedHashMap<String, Group>()
    private val adHocGroupKeys = HashMap<String, String>()   // "aId|bId" -> groupId

    fun createGroup(
        name: String,
        members: Collection<User>,
        simplified: Boolean = false
    ): Group {
        val group = Group(name = name, members = members, isSimplified = simplified)
        groups[group.id] = group
        return group
    }

    fun group(groupId: String): Group =
        groups[groupId] ?: throw IllegalArgumentException("No group $groupId")

    fun groups(): List<Group> = groups.values.toList()

    /** Find or create the two-person container for a one-off expense. */
    fun oneOnOne(a: User, b: User): Group {
        val key = listOf(a.id, b.id).sorted().joinToString("|")
        adHocGroupKeys[key]?.let { return groups.getValue(it) }
        val group = Group(name = "${a.name} & ${b.name}", members = listOf(a, b))
        groups[group.id] = group
        adHocGroupKeys[key] = group.id
        return group
    }

    fun settleUp(group: Group, from: User, to: User, amount: Money): Expense =
        group.addExpense(Expenses.settlement(from, to, amount))

    /** Sentence 6 of the story: "Alice opens the app and sees she is owed 1600." */
    fun summaryFor(user: User): UserSummary {
        var owed = 0L
        var owing = 0L
        val perGroup = LinkedHashMap<String, Money>()

        for (group in groups.values) {
            if (user !in group.members) continue
            val balance = group.balanceOf(user)
            if (balance.paise == 0L) continue
            perGroup[group.name] = balance
            if (balance.paise > 0) owed += balance.paise else owing += -balance.paise
        }

        return UserSummary(user, Money(owed), Money(owing), perGroup)
    }
}
