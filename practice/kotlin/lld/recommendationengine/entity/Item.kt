package lld.recommendationengine.entity

data class Item(
    val id: String,
    val name: String,
    val category: String,
    val tags: Set<String>,
    val popularity: Double,
    val inStock: Boolean = true,
)
