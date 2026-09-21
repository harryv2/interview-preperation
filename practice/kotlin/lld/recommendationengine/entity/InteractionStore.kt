package lld.recommendationengine.entity

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class InteractionStore {
    private val byUser = ConcurrentHashMap<String, MutableList<Interaction>>()
    private val byItem = ConcurrentHashMap<String, MutableList<Interaction>>()

    fun record(interaction: Interaction) {
        byUser.computeIfAbsent(interaction.userId) { CopyOnWriteArrayList() }.add(interaction)
        byItem.computeIfAbsent(interaction.itemId) { CopyOnWriteArrayList() }.add(interaction)
    }

    fun byUser(userId: String): List<Interaction> {
        return byUser[userId]?.toList() ?: emptyList()
    }

    fun byItem(itemId: String): List<Interaction> {
        return byItem[itemId]?.toList() ?: emptyList()
    }
}
