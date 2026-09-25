package lld.fintech.splitwise.entity

// =====================================================================
//  SETTLEMENT PLAN
// =====================================================================

data class Transfer(val from: User, val to: User, val amount: Money) {
    override fun toString(): String = "${from.name} pays ${to.name} $amount"
}
