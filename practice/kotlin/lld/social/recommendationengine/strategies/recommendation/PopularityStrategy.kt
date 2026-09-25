package lld.social.recommendationengine.strategies.recommendation

import lld.social.recommendationengine.entity.ItemCatalog
import lld.social.recommendationengine.entity.ScoredItem
import lld.social.recommendationengine.entity.User

class PopularityStrategy(private val catalog: ItemCatalog) : RecommendationStrategy {
    override fun recommend(user: User): List<ScoredItem> {
        return catalog.all().map { ScoredItem(it, it.popularity, "popular right now") }
    }
}
