package lld.rippling.rbachierarchy

import lld.rippling.rbachierarchy.entity.Grant
import lld.rippling.rbachierarchy.entity.Resource
import lld.rippling.rbachierarchy.entity.Tenant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class AccessControl {
    private class CachedPrincipals(val version: Long, val ids: Set<String>)

    private val tenants = HashMap<String, Tenant>()
    private val principalCache = ConcurrentHashMap<Pair<String, String>, CachedPrincipals>()
    private val lock = ReentrantReadWriteLock()

    fun addTenant(tenantId: String) {
        lock.write {
            tenants.putIfAbsent(tenantId, Tenant(tenantId))
        }
    }

    fun defineRole(tenantId: String, role: String, permissions: Set<String>) {
        mutate(tenantId) { tenant ->
            tenant.roles[role] = permissions
        }
    }

    fun addResource(tenantId: String, resourceId: String, parentId: String? = null) {
        mutate(tenantId) { tenant ->
            if (parentId != null) {
                require(tenant.resources.containsKey(parentId)) { "No resource $parentId" }
            }
            tenant.resources[resourceId] = Resource(resourceId, parentId)
        }
    }

    fun addGroupToGroup(tenantId: String, childGroup: String, parentGroup: String) {
        mutate(tenantId) { tenant ->
            require(childGroup !in ancestorGroups(tenant, setOf(parentGroup))) { "Group cycle $childGroup -> $parentGroup" }
            tenant.groupParents.getOrPut(childGroup) { HashSet() }.add(parentGroup)
        }
    }

    fun addUserToGroup(tenantId: String, userId: String, group: String) {
        mutate(tenantId) { tenant ->
            tenant.userGroups.getOrPut(userId) { HashSet() }.add(group)
        }
    }

    fun removeUserFromGroup(tenantId: String, userId: String, group: String) {
        mutate(tenantId) { tenant ->
            tenant.userGroups[userId]?.remove(group)
        }
    }

    fun grant(tenantId: String, principalId: String, role: String, resourceId: String) {
        mutate(tenantId) { tenant ->
            require(tenant.roles.containsKey(role)) { "No role $role" }
            require(tenant.resources.containsKey(resourceId)) { "No resource $resourceId" }
            tenant.grants.getOrPut(resourceId) { ArrayList() }.add(Grant(principalId, role))
        }
    }

    fun check(tenantId: String, userId: String, permission: String, resourceId: String): Boolean {
        lock.read {
            val tenant = tenants[tenantId] ?: return false
            val principals = principalsOf(tenant, userId)

            var current = tenant.resources[resourceId]
            while (current != null) {
                for (grant in tenant.grants[current.id].orEmpty()) {
                    if (grant.principalId in principals && permission in tenant.roles[grant.role].orEmpty()) {
                        return true
                    }
                }
                current = current.parentId?.let { tenant.resources[it] }
            }
            return false
        }
    }

    private fun principalsOf(tenant: Tenant, userId: String): Set<String> {
        val key = tenant.id to userId
        val cached = principalCache[key]
        if (cached != null && cached.version == tenant.version) {
            return cached.ids
        }
        val ids = HashSet<String>()
        ids += userId
        ids += ancestorGroups(tenant, tenant.userGroups[userId].orEmpty())
        principalCache[key] = CachedPrincipals(tenant.version, ids)
        return ids
    }

    private fun ancestorGroups(tenant: Tenant, start: Set<String>): Set<String> {
        val seen = HashSet<String>()
        val queue = ArrayDeque(start)
        while (queue.isNotEmpty()) {
            val group = queue.removeFirst()
            if (seen.add(group)) {
                queue.addAll(tenant.groupParents[group].orEmpty())
            }
        }
        return seen
    }

    private fun mutate(tenantId: String, change: (Tenant) -> Unit) {
        lock.write {
            val tenant = requireNotNull(tenants[tenantId]) { "No tenant $tenantId" }
            change(tenant)
            tenant.version++
        }
    }
}
