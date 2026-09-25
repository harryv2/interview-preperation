package lld.referral.entity

import java.time.Instant


enum class ReferralStatus {
    PENDING,
    QUALIFIED
}


class Referral(
    val id: String,
    val referrerId: String,
    val refereeId: String,
    val code: String,
    val createdAt: Instant
) {

    var status: ReferralStatus = ReferralStatus.PENDING
        private set

    var qualifiedAt: Instant? = null
        private set

    fun qualify(at: Instant) {
        check(status == ReferralStatus.PENDING) { "Referral $id already qualified" }
        status = ReferralStatus.QUALIFIED
        qualifiedAt = at
    }

    override fun toString(): String {
        return "Referral $referrerId -> $refereeId [$status]"
    }
}


data class Payout(
    val id: String,
    val userId: String,
    val referralId: String,
    val tier: Int,
    val amount: Money,
    val at: Instant
) {
    override fun toString(): String {
        return "Payout $amount to $userId (tier $tier, from $referralId)"
    }
}
