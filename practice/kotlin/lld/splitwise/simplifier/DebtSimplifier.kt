package lld.splitwise.simplifier

import lld.splitwise.entity.Money
import lld.splitwise.entity.Transfer
import lld.splitwise.entity.User

fun interface DebtSimplifier {
    fun simplify(balances: Map<User, Money>): List<Transfer>
}
