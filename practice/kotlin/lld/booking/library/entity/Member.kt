package lld.booking.library.entity


enum class Tier(val loanLimit: Int, val loanDays: Long) {
    BASIC(3, 14),
    PREMIUM(10, 28)
}


class Member(
    val id: String,
    val name: String,
    val tier: Tier
) {

    var outstandingFine: Money = Money.ZERO
        private set

    fun addFine(amount: Money) {
        outstandingFine += amount
    }

    fun payFine(amount: Money) {
        require(amount <= outstandingFine) { "$name owes only $outstandingFine" }
        outstandingFine -= amount
    }

    override fun toString(): String {
        return "$name ($tier, owes $outstandingFine)"
    }
}
