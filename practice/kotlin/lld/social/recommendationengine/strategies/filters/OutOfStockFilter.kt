package lld.social.recommendationengine.strategies.filters

import lld.social.recommendationengine.entity.ScoredItem
import lld.social.recommendationengine.entity.User

class OutOfStockFilter : Filter {
    override fun apply(user: User, items: List<ScoredItem>): List<ScoredItem> {
        return items.filter { it.item.inStock }
    }
}
