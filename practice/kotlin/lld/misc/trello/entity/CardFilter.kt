package lld.misc.trello.entity

import java.time.Instant


// Composable predicates. A board holds hundreds of cards, not millions, so search is a scan and the design
// question is how filters combine, not how to index them.
fun interface CardFilter {

    fun matches(card: Card): Boolean

    infix fun and(other: CardFilter) = CardFilter { matches(it) && other.matches(it) }

    infix fun or(other: CardFilter) = CardFilter { matches(it) || other.matches(it) }

    operator fun not() = CardFilter { !matches(it) }
}


object Filters {

    fun inList(listId: String) = CardFilter { it.listId == listId }

    fun assignedTo(userId: String) = CardFilter { it.assignees.contains(userId) }

    fun unassigned() = CardFilter { it.assignees.isEmpty() }

    fun labelled(label: String) = CardFilter { it.labels.contains(label) }

    fun textContains(term: String) = CardFilter {
        it.title.contains(term, ignoreCase = true) || it.description.contains(term, ignoreCase = true)
    }

    fun dueBefore(at: Instant) = CardFilter { card -> card.dueAt?.let { it < at } ?: false }

    fun overdue(now: Instant) = CardFilter { it.isOverdue(now) }
}
