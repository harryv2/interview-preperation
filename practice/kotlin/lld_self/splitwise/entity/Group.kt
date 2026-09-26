package lld_self.splitwise.entity

import kotlin.uuid.Uuid


class Group(
    val id: Uuid,
    val name: String,
    val members: Set<User>,
    val simplified: Boolean,
) {

    private val _expenses = mutableListOf<Expense>()
    val expenses: List<Expense> get() = _expenses

    val simplifier: DebtSimplifier = GreedySimplifier()

    fun addExpense(expense: Expense) {
        expense.participants.forEach {
            require(it in members) { "Participants should be group member" }
        }

        _expenses.add(expense)
    }


    fun getBalances(): Map<User, Money> {
        val userBalanceMap = mutableMapOf<User, Money>()

        expenses.forEach {
            it.participants.forEach { p ->
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
        return getBalances()[user] ?: Money.ZERO
    }

    fun getTransfers(): List<Transfer> {
        if(simplified) {
            return getSimplifiedTransfers()
        }

        return getRawTransfers()
    }


    // Splitwise without "simplify debts": per expense, each person's net (paid - share) is computed
    // and those with a negative net pay those with a positive net, matched within that expense.
    // Between any two users only the net of A->B and B->A is shown; nothing is rerouted via a third user.
    private fun getRawTransfers(): List<Transfer> {
        val owed = mutableMapOf<Pair<User, User>, Money>()

        for (expense in expenses) {
            val net = expense.participants.associateWith { expense.netFor(it) }
            val creditors = net.filterValues { it > Money.ZERO }.toMutableMap()

            for ((debtor, balance) in net) {
                var debt = -balance

                while (debt > Money.ZERO) {
                    val (creditor, credit) = creditors.entries.first()
                    val paid = minOf(debt, credit)

                    owed[debtor to creditor] = owed.getOrDefault(debtor to creditor, Money.ZERO) + paid
                    debt -= paid

                    if (paid == credit) {
                        creditors.remove(creditor)
                    } else{
                        creditors[creditor] = credit - paid
                    }
                }
            }
        }

        val transfers = mutableListOf<Transfer>()
        for ((pair, amount) in owed) {
            val (from, to) = pair
            val reverse = owed.getOrDefault(to to from, Money.ZERO)
            if (amount > reverse) {
                transfers.add(Transfer(from, to, amount - reverse))
            }
        }
        return transfers
    }

    private fun getSimplifiedTransfers(): List<Transfer> {
        return simplifier.simplify(getBalances())
    }
}