package lld.misc.trello.entity

import java.time.Instant


class User(val id: String, val name: String) {
    override fun toString(): String = name
}


class Card(
    val id: String,
    var title: String,
    listId: String
) {

    // which list holds it, set by CardList so the two cannot drift apart
    var listId: String = listId
        internal set

    // sparse position inside its list, see CardList
    var position: Long = 0
        internal set

    var description: String = ""
    var dueAt: Instant? = null

    val assignees = mutableSetOf<String>()
    val labels = mutableSetOf<String>()

    fun isOverdue(now: Instant): Boolean {
        val due = dueAt ?: return false
        return now > due
    }

    override fun toString(): String {
        val who = if (assignees.isEmpty()) "" else " (${assignees.size} assigned)"
        return "$title$who"
    }
}
