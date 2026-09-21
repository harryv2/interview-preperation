package lld.recommendationengine.strategies.recommendation

import lld.recommendationengine.entity.InteractionStore
import lld.recommendationengine.entity.ItemCatalog
import lld.recommendationengine.entity.ScoredItem
import lld.recommendationengine.entity.User

class CollaborativeStrategy(
    private val catalog: ItemCatalog,
    private val interactions: InteractionStore,
) : RecommendationStrategy {

    override fun recommend(user: User): List<ScoredItem> {
        val myItems = interactions.byUser(user.id).map { it.itemId }.toSet()

        val similarUsers = HashMap<String, Int>()
        for (itemId in myItems) {
            for (other in interactions.byItem(itemId)) {
                if (other.userId != user.id) similarUsers[other.userId] = (similarUsers[other.userId] ?: 0) + 1
            }
        }

        val score = HashMap<String, Double>()
        for ((otherId, overlap) in similarUsers) {
            for (interaction in interactions.byUser(otherId)) {
                if (interaction.itemId in myItems) continue
                score[interaction.itemId] = (score[interaction.itemId] ?: 0.0) + overlap * interaction.type.weight
            }
        }

        return score.map { (itemId, s) -> ScoredItem(catalog.get(itemId), s, "people like you also liked this") }
    }
}
