package lld.misc.trello.entity

import java.time.Instant
import java.util.UUID


// The aggregate root. A move touches two lists and the card index at once, so the board owns both and is the
// only thing that mutates them.
class Board(
    val id: String,
    val name: String
) {

    private val lists = LinkedHashMap<String, CardList>()
    private val cardsById = mutableMapOf<String, Card>()

    fun lists(): List<CardList> = lists.values.toList()

    fun list(listId: String): CardList {
        return requireNotNull(lists[listId]) { "Board $name has no list $listId" }
    }

    // O(1), because a card is opened by id far more often than a list is walked
    fun card(cardId: String): Card {
        return requireNotNull(cardsById[cardId]) { "Board $name has no card $cardId" }
    }

    fun addList(listName: String): CardList {
        val list = CardList(newId("cl"), listName)
        lists[list.id] = list
        return list
    }

    fun addCard(listId: String, title: String, index: Int = Int.MAX_VALUE): Card {
        require(title.isNotBlank()) { "A card needs a title" }

        val card = Card(newId("c"), title, listId)
        list(listId).add(card, index)
        cardsById[card.id] = card
        return card
    }

    // only the moved card's position is written, the cards it lands between do not change
    fun moveCard(cardId: String, toListId: String, toIndex: Int): Card {
        val card = card(cardId)
        val target = list(toListId)

        if (card.listId == toListId) {
            target.moveWithin(cardId, toIndex)
            return card
        }

        list(card.listId).remove(cardId)
        target.add(card, toIndex)
        return card
    }

    fun assign(cardId: String, userId: String): Card {
        val card = card(cardId)
        card.assignees.add(userId)
        return card
    }

    fun unassign(cardId: String, userId: String): Card {
        val card = card(cardId)
        card.assignees.remove(userId)
        return card
    }

    fun search(filter: CardFilter): List<Card> {
        return cardsById.values.filter { filter.matches(it) }.sortedBy { it.title }
    }

    fun overdue(now: Instant): List<Card> {
        return search(Filters.overdue(now)).sortedBy { it.dueAt }
    }

    private fun newId(prefix: String): String {
        return "$prefix-${UUID.randomUUID().toString().take(6)}"
    }

    override fun toString(): String {
        return "$name (${lists.size} lists, ${cardsById.size} cards)"
    }
}
