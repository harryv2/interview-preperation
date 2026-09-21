package lld.recommendationengine.entity

enum class InteractionType(val weight: Double) {
    VIEW(1.0),
    LIKE(3.0),
    PURCHASE(5.0),
}

data class Interaction(
    val userId: String,
    val itemId: String,
    val type: InteractionType,
)
