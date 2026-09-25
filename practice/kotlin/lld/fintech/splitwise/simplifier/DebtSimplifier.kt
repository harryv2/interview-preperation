package lld.fintech.splitwise.simplifier

import lld.fintech.splitwise.entity.Money
import lld.fintech.splitwise.entity.Transfer
import lld.fintech.splitwise.entity.User

fun interface DebtSimplifier {
    fun simplify(balances: Map<User, Money>): List<Transfer>
}
