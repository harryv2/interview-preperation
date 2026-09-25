package lld.misc.trello.entity


// Cards are laid out GAP apart instead of 1 apart, so dropping one between two others is the midpoint of their
// positions and writes one row. A dense 0,1,2,3 column would renumber every card below the drop.
class CardList(
    val id: String,
    var name: String
) {

    private val cards = mutableMapOf<String, Card>()

    val size: Int
        get() = cards.size

    // position first, card id to break ties, because two clients can compute the same midpoint offline
    fun cards(): List<Card> {
        return cards.values.sortedWith(compareBy({ it.position }, { it.id }))
    }

    fun add(card: Card, index: Int) {
        card.position = positionAt(index, null)
        card.listId = id
        cards[card.id] = card
    }

    fun remove(cardId: String): Card {
        return requireNotNull(cards.remove(cardId)) { "List $name has no card $cardId" }
    }

    fun moveWithin(cardId: String, toIndex: Int) {
        val card = requireNotNull(cards[cardId]) { "List $name has no card $cardId" }
        card.position = positionAt(toIndex, cardId)
    }

    fun positionAt(index: Int, excluding: String?): Long {
        val current = cards().filter { it.id != excluding }
        val slot = index.coerceIn(0, current.size)

        val before = current.getOrNull(slot - 1)?.position ?: 0
        val after = current.getOrNull(slot)?.position ?: return before + GAP

        // the gap closed, spread the list out and ask again. One retry is enough, the gaps are GAP wide now.
        if (after - before <= 1) {
            respace()
            return positionAt(index, excluding)
        }
        return before + (after - before) / 2
    }

    // the one operation that writes every row, which is the whole cost of the scheme
    fun respace() {
        cards().forEachIndexed { slot, card -> card.position = (slot + 1) * GAP }
    }

    fun positions(): List<Long> {
        return cards().map { it.position }
    }

    override fun toString(): String {
        return "$name [$size]"
    }

    companion object {
        const val GAP = 100L
    }
}
