package lld.rippling.accessmanagement

import lld.rippling.accessmanagement.entity.Action
import lld.rippling.accessmanagement.entity.AuditEvent
import lld.rippling.accessmanagement.entity.AuditType
import lld.rippling.accessmanagement.entity.Effect
import lld.rippling.accessmanagement.entity.Group
import lld.rippling.accessmanagement.entity.Permission
import lld.rippling.accessmanagement.entity.Role
import lld.rippling.accessmanagement.entity.User
import java.time.Instant

class AccessManager(private val clock: () -> Instant = Instant::now) {
    private val users = HashMap<String, User>()
    private val groups = HashMap<String, Group>()
    private val roles = HashMap<String, Role>()

    val audit = AuditLog()

    fun addUser(userId: String) {
        users.putIfAbsent(userId, User(userId))
    }

    fun addGroup(name: String) {
        groups.putIfAbsent(name, Group(name))
    }

    fun addRole(name: String, vararg permissions: Permission, actor: String = "system") {
        val role = roles.getOrPut(name) { Role(name) }
        role.permissions += permissions
        log(actor, AuditType.ROLE_DEFINED, name, permissions.joinToString { "${it.action} ${it.resource}" })
    }

    fun assignRoleToUser(userId: String, roleName: String, expiresAt: Instant? = null, actor: String = "system") {
        if (expiresAt != null) {
            require(expiresAt.isAfter(clock())) { "Expiry $expiresAt is in the past" }
        }
        user(userId).roles[role(roleName).name] = expiresAt
        log(actor, AuditType.ROLE_GRANTED, userId, if (expiresAt == null) roleName else "$roleName until $expiresAt")
    }

    fun revokeRoleFromUser(userId: String, roleName: String, actor: String = "system") {
        if (!user(userId).roles.containsKey(roleName)) {
            return
        }
        user(userId).roles.remove(roleName)
        log(actor, AuditType.ROLE_REVOKED, userId, roleName)
    }

    fun assignRoleToGroup(groupName: String, roleName: String, actor: String = "system") {
        group(groupName).roles += role(roleName).name
        log(actor, AuditType.GROUP_ROLE_GRANTED, groupName, roleName)
    }

    fun addUserToGroup(userId: String, groupName: String, actor: String = "system") {
        user(userId).groups += group(groupName).name
        log(actor, AuditType.GROUP_JOINED, userId, groupName)
    }

    fun removeUserFromGroup(userId: String, groupName: String, actor: String = "system") {
        if (!user(userId).groups.remove(groupName)) {
            return
        }
        log(actor, AuditType.GROUP_LEFT, userId, groupName)
    }

    fun addGroupToGroup(childGroup: String, parentGroup: String, actor: String = "system") {
        group(childGroup)
        group(parentGroup)
        require(childGroup !in groupsWithAncestors(setOf(parentGroup))) {
            "$parentGroup is already inside $childGroup, nesting would make a cycle"
        }
        group(childGroup).parents += parentGroup
        log(actor, AuditType.GROUP_NESTED, childGroup, "inside $parentGroup")
    }

    fun removeGroupFromGroup(childGroup: String, parentGroup: String, actor: String = "system") {
        if (!group(childGroup).parents.remove(parentGroup)) {
            return
        }
        log(actor, AuditType.GROUP_UNNESTED, childGroup, "out of $parentGroup")
    }

    fun groupsOf(userId: String): Set<String> {
        return groupsWithAncestors(user(userId).groups)
    }

    fun activeRoles(userId: String): Set<String> {
        val user = user(userId)
        val now = clock()
        val result = HashSet<String>()
        for ((roleName, expiresAt) in user.roles) {
            if (expiresAt == null || expiresAt.isAfter(now)) {
                result += roleName
            }
        }
        for (groupName in groupsWithAncestors(user.groups)) {
            result += group(groupName).roles
        }
        return result
    }

    fun effectivePermissions(userId: String): Set<Permission> {
        return activeRoles(userId).flatMap { role(it).permissions }.toSet()
    }

    fun hasAccess(userId: String, action: Action, resource: String): Boolean {
        val matching = effectivePermissions(userId).filter { it.matches(action, resource) }
        if (matching.any { it.effect == Effect.DENY }) {
            return false
        }
        return matching.any { it.effect == Effect.ALLOW }
    }

    fun explain(userId: String, action: Action, resource: String): List<Pair<String, Permission>> {
        val result = ArrayList<Pair<String, Permission>>()
        for (roleName in activeRoles(userId)) {
            for (permission in role(roleName).permissions) {
                if (permission.matches(action, resource)) {
                    result += roleName to permission
                }
            }
        }
        return result
    }

    fun purgeExpired(): Int {
        val now = clock()
        var removed = 0
        for (user in users.values) {
            val expired = user.roles.filterValues { it != null && !it.isAfter(now) }.keys
            for (roleName in expired) {
                user.roles.remove(roleName)
                log("system", AuditType.ROLE_EXPIRED, user.id, roleName)
                removed++
            }
        }
        return removed
    }

    private fun groupsWithAncestors(start: Set<String>): Set<String> {
        val seen = HashSet<String>()
        val stack = ArrayDeque(start)
        while (stack.isNotEmpty()) {
            val name = stack.removeLast()
            if (seen.add(name)) {
                stack.addAll(group(name).parents)
            }
        }
        return seen
    }

    private fun log(actor: String, type: AuditType, subject: String, detail: String) {
        audit.record(AuditEvent(clock(), actor, type, subject, detail))
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
