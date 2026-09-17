package lld_self.splitwise.entity

import kotlin.uuid.Uuid

class User(
    val id: Uuid,
    val name: String
) {

    // entity identity: two Users are the same user iff they share an id
    override fun equals(other: Any?) = other is User && other.id == id
    override fun hashCode() = id.hashCode()

    override fun toString(): String {
        return "$name"
    }
}
