package lld.social.recommendationengine.strategies.recommendation

import lld.social.recommendationengine.entity.InteractionStore
import lld.social.recommendationengine.entity.ItemCatalog
import lld.social.recommendationengine.entity.ScoredItem
import lld.social.recommendationengine.entity.User

class ContentBasedStrategy(
    private val catalog: ItemCatalog,
    private val interactions: InteractionStore,
) : RecommendationStrategy {

    override fun recommend(user: User): List<ScoredItem> {
        val tagWeight = HashMap<String, Double>()
        for (interaction in interactions.byUser(user.id)) {
            for (tag in catalog.get(interaction.itemId).tags) {
                tagWeight[tag] = (tagWeight[tag] ?: 0.0) + interaction.type.weight
            }
        }

        return catalog.all()
            .map { item ->
                val score = item.tags.sumOf { tagWeight[it] ?: 0.0 }
                val topTag = item.tags.maxByOrNull { tagWeight[it] ?: 0.0 }
                ScoredItem(item, score, "you like $topTag")
            }
            .filter { it.score > 0 }
    }
}
