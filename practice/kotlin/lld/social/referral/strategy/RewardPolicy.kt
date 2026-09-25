package lld.social.referral.strategy

import lld.social.referral.entity.Money


interface RewardPolicy {
    fun rewardForTier(tier: Int): Money?
    fun maxTier(): Int
}


class FlatTieredReward(
    private val tiers: List<Money>
) : RewardPolicy {

    init {
        require(tiers.isNotEmpty()) { "Need at least one tier" }
    }

    override fun rewardForTier(tier: Int): Money? {
        return tiers.getOrNull(tier - 1)
    }

    override fun maxTier(): Int {
        return tiers.size
    }
}
