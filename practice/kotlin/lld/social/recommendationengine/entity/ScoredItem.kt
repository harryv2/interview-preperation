package lld.social.recommendationengine.entity

data class ScoredItem(
    val item: Item,
    val score: Double,
    val reason: String,
)
