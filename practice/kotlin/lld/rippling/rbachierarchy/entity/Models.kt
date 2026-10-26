package lld.rippling.rbachierarchy.entity

data class Resource(
    val id: String,
    val parentId: String?,
)

data class Grant(
    val principalId: String,
    val role: String,
)

class Tenant(val id: String) {
    val roles = HashMap<String, Set<String>>()
    val resources = HashMap<String, Resource>()
    val groupParents = HashMap<String, HashSet<String>>()
    val userGroups = HashMap<String, HashSet<String>>()
    val grants = HashMap<String, ArrayList<Grant>>()
    var version = 0L
}
