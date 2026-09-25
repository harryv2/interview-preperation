package lld.social.recommendationengine

import lld.social.recommendationengine.entity.Interaction
import lld.social.recommendationengine.entity.InteractionStore
import lld.social.recommendationengine.entity.InteractionType.LIKE
import lld.social.recommendationengine.entity.InteractionType.PURCHASE
import lld.social.recommendationengine.entity.InteractionType.VIEW
import lld.social.recommendationengine.entity.Item
import lld.social.recommendationengine.entity.ItemCatalog
import lld.social.recommendationengine.entity.RecommendationService
import lld.social.recommendationengine.entity.ScoredItem
import lld.social.recommendationengine.entity.User
import lld.social.recommendationengine.strategies.filters.AlreadyPurchasedFilter
import lld.social.recommendationengine.strategies.filters.OutOfStockFilter
import lld.social.recommendationengine.strategies.recommendation.CollaborativeStrategy
import lld.social.recommendationengine.strategies.recommendation.ContentBasedStrategy
import lld.social.recommendationengine.strategies.recommendation.HybridStrategy
import lld.social.recommendationengine.strategies.recommendation.PopularityStrategy

fun main() {
    val catalog = ItemCatalog(
        listOf(
            Item("i1", "Running shoes", "sports", setOf("running", "shoes"), popularity = 90.0),
            Item("i2", "Yoga mat", "sports", setOf("yoga", "fitness"), popularity = 70.0),
            Item("i3", "Trail shoes", "sports", setOf("running", "shoes", "outdoor"), popularity = 40.0),
            Item("i4", "Protein powder", "nutrition", setOf("fitness", "protein"), popularity = 85.0),
            Item("i5", "Camping tent", "outdoor", setOf("outdoor", "camping"), popularity = 30.0),
            Item("i6", "Smart watch", "electronics", setOf("running", "fitness", "gadget"), popularity = 95.0, inStock = false),
        ),
    )
    val interactions = InteractionStore()
    listOf(
        Interaction("alice", "i1", PURCHASE), Interaction("alice", "i3", VIEW),
        Interaction("bob", "i1", PURCHASE), Interaction("bob", "i3", LIKE), Interaction("bob", "i5", PURCHASE),
        Interaction("carol", "i2", PURCHASE), Interaction("carol", "i4", PURCHASE),
    ).forEach { interactions.record(it) }

    val popularity = PopularityStrategy(catalog)
    val content = ContentBasedStrategy(catalog, interactions)
    val collaborative = CollaborativeStrategy(catalog, interactions)
    val hybrid = HybridStrategy(mapOf(content to 0.5, collaborative to 0.4, popularity to 0.1))

    val filters = listOf(AlreadyPurchasedFilter(interactions), OutOfStockFilter())
    val alice = User("alice", "Alice")
    val newUser = User("dave", "Dave")

    show("alice / content-based", RecommendationService(content, popularity, filters, interactions).recommend(alice, 3))
    show("alice / collaborative", RecommendationService(collaborative, popularity, filters, interactions).recommend(alice, 3))
    show("alice / hybrid", RecommendationService(hybrid, popularity, filters, interactions).recommend(alice, 3))
    show("dave (new user) / hybrid falls back to popularity", RecommendationService(hybrid, popularity, filters, interactions).recommend(newUser, 3))
}

private fun show(title: String, items: List<ScoredItem>) {
    println(title)
    items.forEach { println("  %-16s %.2f  %s".format(it.item.name, it.score, it.reason)) }
}
