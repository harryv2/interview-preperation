package lld.rippling.accessmanagement

import lld.rippling.accessmanagement.entity.Action
import lld.rippling.accessmanagement.entity.AuditType
import lld.rippling.accessmanagement.entity.Effect
import lld.rippling.accessmanagement.entity.Permission
import java.time.Duration
import java.time.Instant

fun main() {
    var now = Instant.parse("2026-10-01T09:00:00Z")
    val access = AccessManager { now }

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

    println("-- time-bound grant: contractor gets payroll admin for 2 days")
    access.addUser("frank")
    access.assignRoleToUser("frank", "payroll_admin", expiresAt = now.plus(Duration.ofDays(2)), actor = "carol")
    println("  day 0 frank write payroll: ${access.hasAccess("frank", Action.WRITE, "payroll")}")
    now = now.plus(Duration.ofDays(1))
    println("  day 1 frank write payroll: ${access.hasAccess("frank", Action.WRITE, "payroll")}")
    now = now.plus(Duration.ofDays(1))
    println("  day 2 frank write payroll: ${access.hasAccess("frank", Action.WRITE, "payroll")} (expired, checked lazily)")
    println("  purged expired links: ${access.purgeExpired()}")

    println("-- re-granting extends, past expiry is rejected")
    access.assignRoleToUser("frank", "payroll_viewer", expiresAt = now.plus(Duration.ofHours(8)), actor = "carol")
    println("  frank read payroll: ${access.hasAccess("frank", Action.READ, "payroll")}")
    runCatching { access.assignRoleToUser("frank", "payroll_viewer", expiresAt = now.minusSeconds(1)) }
        .onFailure { println("  ${it.message}") }

    println("-- audit trail for frank")
    for (event in access.audit.forSubject("frank")) {
        println("  ${event.at} ${event.actor} ${event.type} ${event.detail}")
    }

    println("-- everything carol did")
    println("  ${access.audit.byActor("carol").map { "${it.type} ${it.subject}" }}")

    println("-- expiries on 2026-10-03")
    val dayTwo = Instant.parse("2026-10-03T00:00:00Z")
    val expiries = access.audit.between(dayTwo, dayTwo.plus(Duration.ofDays(1)), AuditType.ROLE_EXPIRED)
    println("  ${expiries.map { "${it.subject} ${it.detail}" }}")
    println("  total events logged: ${access.audit.all().size}")

    println("-- nested groups: payroll-team inside finance inside all-staff")
    access.addRole("staff_reader", Permission(Action.READ, "handbook"))
    access.addGroup("all-staff")
    access.addGroup("payroll-team")
    access.addGroupToGroup("finance", "all-staff")
    access.addGroupToGroup("payroll-team", "finance")
    access.assignRoleToGroup("all-staff", "staff_reader")
    access.addUser("gina")
    access.addUserToGroup("gina", "payroll-team")
    println("  gina groups: ${access.groupsOf("gina").sorted()}")
    println("  gina read handbook (from all-staff): ${access.hasAccess("gina", Action.READ, "handbook")}")
    println("  gina read payroll (from finance):    ${access.hasAccess("gina", Action.READ, "payroll")}")
    runCatching { access.addGroupToGroup("all-staff", "payroll-team") }.onFailure { println("  ${it.message}") }

    println("-- explicit deny beats allow: finance can read payroll/*, but not payroll/exec/*")
    access.addRole("finance_reader", Permission(Action.READ, "payroll/*"))
    access.addRole("no_exec_pay", Permission(Action.READ, "payroll/exec/*", Effect.DENY))
    access.assignRoleToGroup("finance", "finance_reader")
    access.assignRoleToGroup("payroll-team", "no_exec_pay")
    println("  gina read payroll/us/ca:   ${access.hasAccess("gina", Action.READ, "payroll/us/ca")}")
    println("  gina read payroll/exec/ceo: ${access.hasAccess("gina", Action.READ, "payroll/exec/ceo")}")
    println("  why: ${access.explain("gina", Action.READ, "payroll/exec/ceo").map { (role, p) -> "$role ${p.effect} ${p.resource}" }}")

    println("-- deny also beats a super admin wildcard")
    access.assignRoleToUser("carol", "no_exec_pay")
    println("  carol read payroll/exec/ceo: ${access.hasAccess("carol", Action.READ, "payroll/exec/ceo")}")
    println("  carol read payroll/us/ca:    ${access.hasAccess("carol", Action.READ, "payroll/us/ca")}")
}
