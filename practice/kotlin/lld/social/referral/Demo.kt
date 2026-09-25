package lld.social.referral

import lld.social.referral.entity.Money
import lld.social.referral.service.ReferralService
import lld.social.referral.strategy.FlatTieredReward

fun main() {

    val service = ReferralService(
        FlatTieredReward(
            listOf(
                Money.rupees(100),
                Money.rupees(50),
                Money.rupees(25)
            )
        )
    )

    banner("sign up a chain: asha -> ravi -> kim -> leo")
    val asha = service.signUp("U1", "Asha")
    println("  $asha")
    val ravi = service.signUp("U2", "Ravi", asha.referralCode)
    val kim = service.signUp("U3", "Kim", ravi.referralCode)
    val leo = service.signUp("U4", "Leo", kim.referralCode)
    println("  ${service.referralOf(ravi.id)}")
    println("  ${service.referralOf(kim.id)}")
    println("  ${service.referralOf(leo.id)}")

    banner("Leo places his first order, rewards flow up three tiers")
    service.qualify(leo.id).forEach { println("  $it") }
    println("  Kim earned ${service.earningsOf(kim.id)}")
    println("  Ravi earned ${service.earningsOf(ravi.id)}")
    println("  Asha earned ${service.earningsOf(asha.id)}")

    banner("qualifying again pays nothing")
    println("  payouts on retry: ${service.qualify(leo.id).size}")
    println("  total paid out is still ${service.totalPaidOut()}")

    banner("a short chain only pays the tiers that exist")
    println("  Ravi qualifies, only Asha is upstream")
    service.qualify(ravi.id).forEach { println("  $it") }
    println("  Asha earned ${service.earningsOf(asha.id)}")

    banner("an organic signup with no code earns nobody anything")
    val mia = service.signUp("U5", "Mia")
    println("  referral: ${service.referralOf(mia.id)}")
    println("  payouts:  ${service.qualify(mia.id).size}")

    banner("rejected")
    attempt("sign up with a code that does not exist") {
        service.signUp("U6", "Nina", "NOPE123")
    }
    attempt("reuse an existing user id") {
        service.signUp("U1", "Asha Again")
    }
    attempt("refer yourself") {
        service.applyCode(mia.id, mia.referralCode)
    }
    attempt("a cycle: Asha applies Leo's code, but Leo is downstream of Asha") {
        service.applyCode(asha.id, leo.referralCode)
    }
    attempt("apply a code when already referred") {
        service.applyCode(ravi.id, mia.referralCode)
    }
    attempt("an existing user applies a valid code") {
        service.applyCode(mia.id, asha.referralCode)
    }
    attempt("qualify an unknown user") {
        service.qualify("U99")
    }

    banner("who did Asha bring in")
    service.referralsBy(asha.id).forEach { println("  $it") }
    println("  total paid out across everyone: ${service.totalPaidOut()}")
}

private fun banner(title: String) {
    println("\n== $title ==")
}

private fun attempt(label: String, action: () -> Unit) {
    try {
        action()
        println("  $label -> allowed")
    } catch (e: RuntimeException) {
        println("  $label -> rejected: ${e.message}")
    }
}
