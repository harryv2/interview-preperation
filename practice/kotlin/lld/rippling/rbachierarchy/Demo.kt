package lld.rippling.rbachierarchy

fun main() {
    val acl = AccessControl()
    acl.addTenant("acme")
    acl.addTenant("globex")

    acl.defineRole("acme", "viewer", setOf("read"))
    acl.defineRole("acme", "editor", setOf("read", "write"))

    acl.addResource("acme", "root")
    acl.addResource("acme", "hr", "root")
    acl.addResource("acme", "payroll", "hr")
    acl.addResource("acme", "payroll-us", "payroll")

    acl.addGroupToGroup("acme", "payroll-team", "finance")
    acl.addGroupToGroup("acme", "finance", "all-staff")
    acl.addUserToGroup("acme", "alice", "payroll-team")
    acl.addUserToGroup("acme", "bob", "all-staff")

    acl.grant("acme", "finance", "editor", "payroll")
    acl.grant("acme", "all-staff", "viewer", "hr")

    println("-- nested group + parent resource")
    println("  alice write payroll-us: ${acl.check("acme", "alice", "write", "payroll-us")}")
    println("  alice read hr:          ${acl.check("acme", "alice", "read", "hr")}")
    println("  bob read payroll-us:    ${acl.check("acme", "bob", "read", "payroll-us")}")
    println("  bob write payroll:      ${acl.check("acme", "bob", "write", "payroll")}")

    println("-- tenants are isolated")
    println("  alice read payroll in globex: ${acl.check("globex", "alice", "read", "payroll")}")

    println("-- membership change invalidates the cache")
    acl.removeUserFromGroup("acme", "alice", "payroll-team")
    println("  alice write payroll-us: ${acl.check("acme", "alice", "write", "payroll-us")}")

    println("-- group cycles are rejected")
    runCatching { acl.addGroupToGroup("acme", "all-staff", "payroll-team") }.onFailure { println("  ${it.message}") }
}
