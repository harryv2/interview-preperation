package lld.social.recommendationengine.strategies.filters

import lld.social.recommendationengine.entity.InteractionStore
import lld.social.recommendationengine.entity.InteractionType
import lld.social.recommendationengine.entity.ScoredItem
import lld.social.recommendationengine.entity.User

class AlreadyPurchasedFilter(private val interactions: InteractionStore) : Filter {
    override fun apply(user: User, items: List<ScoredItem>): List<ScoredItem> {
        val purchased = interactions.byUser(user.id)
            .filter { it.type == InteractionType.PURCHASE }
            .map { it.itemId }
            .toSet()
        return items.filter { it.item.id !in purchased }
    }
}
