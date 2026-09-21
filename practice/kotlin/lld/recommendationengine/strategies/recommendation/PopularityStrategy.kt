package lld.recommendationengine.strategies.recommendation

import lld.recommendationengine.entity.ItemCatalog
import lld.recommendationengine.entity.ScoredItem
import lld.recommendationengine.entity.User

class PopularityStrategy(private val catalog: ItemCatalog) : RecommendationStrategy {
    override fun recommend(user: User): List<ScoredItem> {
        return catalog.all().map { ScoredItem(it, it.popularity, "popular right now") }
    }
}
