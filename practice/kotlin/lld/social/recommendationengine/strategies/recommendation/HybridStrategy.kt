package lld.social.recommendationengine.strategies.recommendation

import lld.social.recommendationengine.entity.ScoredItem
import lld.social.recommendationengine.entity.User

class HybridStrategy(private val weights: Map<RecommendationStrategy, Double>) : RecommendationStrategy {

    override fun recommend(user: User): List<ScoredItem> {
        val merged = HashMap<String, ScoredItem>()

        for ((strategy, weight) in weights) {
            for (scored in normalize(strategy.recommend(user))) {
                val previous = merged[scored.item.id]
                val score = (previous?.score ?: 0.0) + scored.score * weight
                merged[scored.item.id] = ScoredItem(scored.item, score, previous?.reason ?: scored.reason)
            }
        }
        return merged.values.toList()
    }

    // scores from different strategies are on different scales, bring each to 0..1 before mixing
    private fun normalize(items: List<ScoredItem>): List<ScoredItem> {
        val max = items.maxOfOrNull { it.score } ?: return items
        if (max == 0.0) return items
        return items.map { it.copy(score = it.score / max) }
    }
}
