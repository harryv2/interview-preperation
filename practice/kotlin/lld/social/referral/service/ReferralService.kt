package lld.social.referral.service

import lld.social.referral.entity.Money
import lld.social.referral.entity.Payout
import lld.social.referral.entity.Referral
import lld.social.referral.entity.ReferralStatus
import lld.social.referral.entity.User
import lld.social.referral.strategy.RewardPolicy
import java.time.Instant
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class ReferralService(
    private val rewardPolicy: RewardPolicy
) {

    private val lock = ReentrantLock()
    private val users = HashMap<String, User>()
    private val usersByCode = HashMap<String, User>()
    private val referralByReferee = HashMap<String, Referral>()
    private val payouts = mutableListOf<Payout>()

    fun signUp(id: String, name: String, usingCode: String? = null): User {
        lock.withLock {
            require(id !in users) { "User $id already exists" }

            val user = User(id, name, codeFor(name, id))
            users[id] = user
            usersByCode[user.referralCode] = user

            if (usingCode != null) {
                link(id, usingCode)
            }

            return user
        }
    }

    // an existing user entering a code later is the path where self referral and cycles
    // are actually reachable, at signup the referee is always a brand new id
    fun applyCode(userId: String, code: String): Referral {
        lock.withLock {
            require(userId in users) { "No user $userId" }
            return link(userId, code)
        }
    }

    private fun link(refereeId: String, code: String): Referral {
        val referrer = usersByCode[code]
        requireNotNull(referrer) { "No referral code $code" }
        require(referrer.id != refereeId) { "You can not refer yourself" }
        require(refereeId !in referralByReferee) { "User $refereeId was already referred" }
        require(!wouldCycle(referrer.id, refereeId)) { "${referrer.id} is already downstream of $refereeId" }

        val referral = Referral(
            id = "R-${UUID.randomUUID().toString().take(6)}",
            referrerId = referrer.id,
            refereeId = refereeId,
            code = code,
            createdAt = Instant.now()
        )
        referralByReferee[refereeId] = referral
        return referral
    }

    fun qualify(refereeId: String): List<Payout> {
        lock.withLock {
            require(refereeId in users) { "No user $refereeId" }

            val referral = referralByReferee[refereeId] ?: return emptyList()
            if (referral.status == ReferralStatus.QUALIFIED) {
                return emptyList()
            }

            val now = Instant.now()
            referral.qualify(now)

            val paid = mutableListOf<Payout>()
            var upstream: String? = referral.referrerId

            for (tier in 1..rewardPolicy.maxTier()) {
                val beneficiary = upstream ?: break
                val reward = rewardPolicy.rewardForTier(tier) ?: break

                val payout = Payout(
                    id = "P-${UUID.randomUUID().toString().take(6)}",
                    userId = beneficiary,
                    referralId = referral.id,
                    tier = tier,
                    amount = reward,
                    at = now
                )
                payouts.add(payout)
                paid.add(payout)

                upstream = referralByReferee[beneficiary]?.referrerId
            }

            return paid
        }
    }

    fun referralOf(refereeId: String): Referral? {
        lock.withLock {
            return referralByReferee[refereeId]
        }
    }

    fun referralsBy(referrerId: String): List<Referral> {
        lock.withLock {
            return referralByReferee.values.filter { it.referrerId == referrerId }
        }
    }

    fun payoutsOf(userId: String): List<Payout> {
        lock.withLock {
            return payouts.filter { it.userId == userId }
        }
    }

    fun earningsOf(userId: String): Money {
        var total = Money.ZERO
        payoutsOf(userId).forEach {
            total += it.amount
        }
        return total
    }

    fun totalPaidOut(): Money {
        lock.withLock {
            var total = Money.ZERO
            payouts.forEach {
                total += it.amount
            }
            return total
        }
    }

    private fun wouldCycle(referrerId: String, refereeId: String): Boolean {
        var current: String? = referrerId
        val seen = mutableSetOf<String>()

        while (current != null) {
            if (current == refereeId) {
                return true
            }
            if (!seen.add(current)) {
                return true
            }
            current = referralByReferee[current]?.referrerId
        }

        return false
    }

    private fun codeFor(name: String, id: String): String {
        return "${name.uppercase().take(3)}$id"
    }
}
