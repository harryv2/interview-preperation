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
}
