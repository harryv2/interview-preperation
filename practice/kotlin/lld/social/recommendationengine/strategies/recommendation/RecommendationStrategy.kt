package lld.social.recommendationengine.strategies.recommendation

import lld.social.recommendationengine.entity.ScoredItem
import lld.social.recommendationengine.entity.User

fun interface RecommendationStrategy {
    fun recommend(user: User): List<ScoredItem>
}
