package lld_self.splitwise.entity

import kotlin.uuid.Uuid

class User(
    val id: Uuid,
    val name: String
) {

    override fun toString(): String {
        return "User($id) $name"
    }
}