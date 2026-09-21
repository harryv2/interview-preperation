package lld.recommendationengine.strategies.recommendation

import lld.recommendationengine.entity.ScoredItem
import lld.recommendationengine.entity.User

fun interface RecommendationStrategy {
    fun recommend(user: User): List<ScoredItem>
}
