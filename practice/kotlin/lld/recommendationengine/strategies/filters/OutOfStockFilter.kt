package lld.recommendationengine.strategies.filters

import lld.recommendationengine.entity.ScoredItem
import lld.recommendationengine.entity.User

class OutOfStockFilter : Filter {
    override fun apply(user: User, items: List<ScoredItem>): List<ScoredItem> {
        return items.filter { it.item.inStock }
    }
}
