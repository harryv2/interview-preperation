package lld.rippling.accessmanagement

import lld.rippling.accessmanagement.entity.Action
import lld.rippling.accessmanagement.entity.Group
import lld.rippling.accessmanagement.entity.Permission
import lld.rippling.accessmanagement.entity.Role
import lld.rippling.accessmanagement.entity.User

class AccessManager {
    private val users = HashMap<String, User>()
    private val groups = HashMap<String, Group>()
    private val roles = HashMap<String, Role>()

    fun addUser(userId: String) {
        users.putIfAbsent(userId, User(userId))
    }

    fun addGroup(name: String) {
        groups.putIfAbsent(name, Group(name))
    }

    fun addRole(name: String, vararg permissions: Permission) {
        val role = roles.getOrPut(name) { Role(name) }
        role.permissions += permissions
    }

    fun assignRoleToUser(userId: String, roleName: String) {
        user(userId).roles += role(roleName).name
    }

    fun revokeRoleFromUser(userId: String, roleName: String) {
        user(userId).roles -= roleName
    }

    fun assignRoleToGroup(groupName: String, roleName: String) {
        group(groupName).roles += role(roleName).name
    }

    fun addUserToGroup(userId: String, groupName: String) {
        user(userId).groups += group(groupName).name
    }

    fun removeUserFromGroup(userId: String, groupName: String) {
        user(userId).groups -= groupName
    }

    fun effectivePermissions(userId: String): Set<Permission> {
        val user = user(userId)
        val roleNames = HashSet(user.roles)
        for (groupName in user.groups) {
            roleNames += group(groupName).roles
        }
        return roleNames.flatMap { role(it).permissions }.toSet()
    }

    fun hasAccess(userId: String, action: Action, resource: String): Boolean {
        return effectivePermissions(userId).any { it.allows(action, resource) }
    }

    private fun user(userId: String): User {
        return requireNotNull(users[userId]) { "No user $userId" }
    }

    private fun group(name: String): Group {
        return requireNotNull(groups[name]) { "No group $name" }
    }

    private fun role(name: String): Role {
        return requireNotNull(roles[name]) { "No role $name" }
    }
}
