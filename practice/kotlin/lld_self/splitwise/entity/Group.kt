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


    // No debt simplification: every ower pays the payer(s) of that expense directly.
    private fun getRawTransfers(): List<Transfer> {
        // owed[(from, to)] = how much `from` owes `to`
        val owed = mutableMapOf<Pair<User, User>, Money>()

        for (expense in expenses) {
            for ((payer, paid) in expense.paidBy) {
                for ((ower, share) in expense.owedBy) {
                    if (ower == payer) continue

                    // ower's share is split across payers in proportion to what each paid
                    val part = Money.paise(share.paise * paid.paise / expense.amount.paise)
                    owed[ower to payer] = owed.getOrDefault(ower to payer, Money.ZERO) + part
                }
            }
        }

        // net A->B against B->A
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