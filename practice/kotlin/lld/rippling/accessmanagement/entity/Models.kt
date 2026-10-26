package lld.rippling.accessmanagement.entity

enum class Action { READ, WRITE, DELETE }

data class Permission(
    val action: Action,
    val resource: String,
) {
    fun allows(action: Action, resource: String): Boolean {
        return this.action == action && (this.resource == "*" || this.resource == resource)
    }
}

class Role(val name: String) {
    val permissions = HashSet<Permission>()
}

class Group(val name: String) {
    val roles = HashSet<String>()
}

class User(val id: String) {
    val roles = HashSet<String>()
    val groups = HashSet<String>()
}
