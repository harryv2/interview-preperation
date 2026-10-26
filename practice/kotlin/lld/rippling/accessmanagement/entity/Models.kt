package lld.rippling.accessmanagement.entity

enum class Action { READ, WRITE, DELETE }

data class Permission(
    val action: Action,
    val resource: String,
) {
    init {
        val star = resource.indexOf('*')
        require(star == -1 || resource == "*" || (resource.endsWith("/*") && star == resource.length - 1)) {
            "Wildcard only as \"*\" or a trailing \"/*\": $resource"
        }
    }

    fun allows(action: Action, resource: String): Boolean {
        if (this.action != action) {
            return false
        }
        if (this.resource == "*") {
            return true
        }
        if (this.resource.endsWith("/*")) {
            val prefix = this.resource.removeSuffix("*")
            return resource.startsWith(prefix) && resource.length > prefix.length
        }
        return this.resource == resource
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
