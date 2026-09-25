package lld.fintech.splitwise.service

import lld.fintech.splitwise.entity.Money
import lld.fintech.splitwise.entity.User

data class UserSummary(
    val user: User,
    val totalOwed: Money,      // what others owe this user
    val totalOwing: Money,     // what this user owes others
    val perGroup: Map<String, Money>
) {
    val net: Money get() = totalOwed - totalOwing
}
