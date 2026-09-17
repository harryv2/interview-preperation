package lld_self.splitwise.entity

import kotlin.uuid.Uuid


class Group(
    val id: Uuid,
    val name: String,
    val members: Array<User>,
    val simplified: Boolean,
) {

    val expenses = mutableListOf<Expense>()
    val memberIds = hashSetOf<Uuid>()

    val simplifier: DebtSimplifier = GreedySimplifier()

    init {
        members.forEach {
            memberIds.add(it.id)
        }
    }

    fun addExpense(expense: Expense) {
        expense.participants.forEach {
            require(memberIds.contains(it.id)) {"Participants should be group member"}
        }

        expenses.add(expense)
    }


    fun getBalances(): Map<User, Money> {
        var userBalanceMap = mutableMapOf<User, Money>()

        expenses.forEach {
            var participants = it.participants

            participants.forEach { p ->
                val balance = userBalanceMap.getOrDefault(p, Money.ZERO)
                userBalanceMap[p] = balance + it.netFor(p)
            }
        }

        check(userBalanceMap.values.sumOf { it.paise } == 0L) {
            "Balances for $name do not sum to zero. Some expense has a broken split."
        }

        return userBalanceMap
    }

    fun getBalance(user: User): Money {
        var balances = getBalances()
        return balances[user] ?: Money.ZERO
    }

    fun getTransfers(): List<Transfer> {
        if(simplified) {
            return getSimplifiedTransfers()
        }

        return getRawTransfers()
    }


    /**
     * Who owes whom, expense by expense, the way Splitwise shows it without "simplify debts".
     * Debts between the same two users are netted (A->B 500 and B->A 200 becomes A->B 300),
     * but a debt is never rerouted through a third user.
     */
    private fun getRawTransfers(): List<Transfer> {
        // Signed net debt per pair, keyed (a, b) with a.id < b.id so both directions share one entry:
        // positive means a owes b, negative means b owes a.
        val ledger = mutableMapOf<Pair<User, User>, Money>()

        fun record(from: User, to: User, amount: Money) {
            if (from.id < to.id) {
                ledger[from to to] = ledger.getOrDefault(from to to, Money.ZERO) + amount
            } else {
                ledger[to to from] = ledger.getOrDefault(to to from, Money.ZERO) - amount
            }
        }

        expenses.forEach { expense ->
            // Payers who put in more than their share, with how much they are still owed.
            val creditors = expense.participants
                .map { it to expense.netFor(it) }
                .filter { (_, net) -> net > Money.ZERO }
                .toMutableList()

            expense.participants.forEach { debtor ->
                var debt = -expense.netFor(debtor)

                // Expense guarantees paid == owed, so creditors cover every debtor exactly.
                while (debt > Money.ZERO) {
                    val (creditor, credit) = creditors.first()
                    val paid = minOf(debt, credit)

                    record(debtor, creditor, paid)
                    debt -= paid

                    if (paid == credit) {
                        creditors.removeAt(0)
                    } else {
                        creditors[0] = creditor to (credit - paid)
                    }
                }
            }
        }

        return ledger.mapNotNull { (pair, net) ->
            val (a, b) = pair
            when {
                net > Money.ZERO -> Transfer(a, b, net)
                net < Money.ZERO -> Transfer(b, a, -net)
                else -> null
            }
        }
    }

    private fun getSimplifiedTransfers(): List<Transfer> {
        var balances = getBalances()
        return simplifier.simplify(balances)
    }
}