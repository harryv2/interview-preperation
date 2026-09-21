package lld.recommendationengine.entity

class ItemCatalog(items: List<Item>) {
    private val items = items.associateBy { it.id }

    fun get(id: String): Item {
        return requireNotNull(items[id]) { "Item $id not found" }
    }

    fun all(): List<Item> {
        return items.values.toList()
    }
}
