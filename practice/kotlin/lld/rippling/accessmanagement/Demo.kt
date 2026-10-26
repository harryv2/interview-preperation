package lld.rippling.accessmanagement

import lld.rippling.accessmanagement.entity.Action
import lld.rippling.accessmanagement.entity.Permission

fun main() {
    val access = AccessManager()

    access.addRole("payroll_viewer", Permission(Action.READ, "payroll"))
    access.addRole("payroll_admin", Permission(Action.READ, "payroll"), Permission(Action.WRITE, "payroll"))
    access.addRole("super_admin", Permission(Action.READ, "*"), Permission(Action.WRITE, "*"), Permission(Action.DELETE, "*"))

    access.addUser("alice")
    access.addUser("bob")
    access.addUser("carol")
    access.addGroup("finance")
    access.assignRoleToGroup("finance", "payroll_viewer")

    access.addUserToGroup("alice", "finance")
    access.assignRoleToUser("bob", "payroll_admin")
    access.assignRoleToUser("carol", "super_admin")

    println("-- direct role, group role, wildcard")
    println("  alice read payroll:  ${access.hasAccess("alice", Action.READ, "payroll")}")
    println("  alice write payroll: ${access.hasAccess("alice", Action.WRITE, "payroll")}")
    println("  bob write payroll:   ${access.hasAccess("bob", Action.WRITE, "payroll")}")
    println("  carol delete devices: ${access.hasAccess("carol", Action.DELETE, "devices")}")

    println("-- leaving the group drops its roles")
    access.removeUserFromGroup("alice", "finance")
    println("  alice read payroll:  ${access.hasAccess("alice", Action.READ, "payroll")}")

    println("-- resource hierarchy: payroll/us/* covers everything under payroll/us")
    access.addRole("us_payroll_admin", Permission(Action.READ, "payroll/us/*"), Permission(Action.WRITE, "payroll/us/*"))
    access.addRole("ca_payroll_viewer", Permission(Action.READ, "payroll/us/ca"))
    access.addUser("dan")
    access.addUser("erin")
    access.assignRoleToUser("dan", "us_payroll_admin")
    access.assignRoleToUser("erin", "ca_payroll_viewer")

    println("  dan write payroll/us/ca:        ${access.hasAccess("dan", Action.WRITE, "payroll/us/ca")}")
    println("  dan read payroll/us/ny/2026-01: ${access.hasAccess("dan", Action.READ, "payroll/us/ny/2026-01")}")
    println("  dan read payroll/us:            ${access.hasAccess("dan", Action.READ, "payroll/us")}")
    println("  dan read payroll/uk/london:     ${access.hasAccess("dan", Action.READ, "payroll/uk/london")}")
    println("  dan read payroll/usa:           ${access.hasAccess("dan", Action.READ, "payroll/usa")}")
    println("  erin read payroll/us/ca:        ${access.hasAccess("erin", Action.READ, "payroll/us/ca")}")
    println("  erin read payroll/us/ca/sf:     ${access.hasAccess("erin", Action.READ, "payroll/us/ca/sf")}")

    println("-- wildcard in the middle is rejected")
    runCatching { Permission(Action.READ, "payroll/*/ca") }.onFailure { println("  ${it.message}") }
}
