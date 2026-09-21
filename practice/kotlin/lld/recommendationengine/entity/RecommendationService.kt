package lld.recommendationengine.entity

import lld.recommendationengine.strategies.filters.Filter
import lld.recommendationengine.strategies.recommendation.RecommendationStrategy

class RecommendationService(
    private val strategy: RecommendationStrategy,
    private val coldStart: RecommendationStrategy,
    private val filters: List<Filter>,
    private val interactions: InteractionStore,
) {
    fun recommend(user: User, limit: Int): List<ScoredItem> {
        val chosen = if (interactions.byUser(user.id).isEmpty()) coldStart else strategy

        var candidates = chosen.recommend(user)
        for (filter in filters) {
            candidates = filter.apply(user, candidates)
        }

        return candidates
            .sortedByDescending { it.score }
            .distinctBy { it.item.id }
            .take(limit)
    }
}
