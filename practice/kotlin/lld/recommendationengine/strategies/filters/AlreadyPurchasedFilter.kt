package lld.recommendationengine.strategies.filters

import lld.recommendationengine.entity.InteractionStore
import lld.recommendationengine.entity.InteractionType
import lld.recommendationengine.entity.ScoredItem
import lld.recommendationengine.entity.User

class AlreadyPurchasedFilter(private val interactions: InteractionStore) : Filter {
    override fun apply(user: User, items: List<ScoredItem>): List<ScoredItem> {
        val purchased = interactions.byUser(user.id)
            .filter { it.type == InteractionType.PURCHASE }
            .map { it.itemId }
            .toSet()
        return items.filter { it.item.id !in purchased }
    }
}
