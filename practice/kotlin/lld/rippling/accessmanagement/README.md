# Access Management (RBAC)

Rippling SDE-1 LLD round. Rippling sells identity / device / app access, so this fits their domain.

- `Permission` — (action, resource pattern, effect `ALLOW` / `DENY`). Patterns:
  - `*` — any resource
  - `payroll/us/*` — everything under `payroll/us` at any depth (`payroll/us/ca`, `payroll/us/ny/2026-01`), but not `payroll/us` itself and not `payroll/usa` (the prefix keeps the trailing `/`, so it matches whole path segments)
  - `payroll/us/ca` — exactly that resource
  - a `*` anywhere else is rejected when the permission is created
- `Role` — named set of permissions
- `Group` — named set of roles + parent groups (a team inside a department inside all-staff)
- `User` — direct roles (role -> optional `expiresAt`) + groups
- `AccessManager` — CRUD for the above, `effectivePermissions(user)` = active direct roles ∪ roles of every group the user is in, directly or through nesting. `hasAccess(user, action, resource)`: any matching `DENY` -> false, else any matching `ALLOW` -> true

Store names, not object references, between user / group / role so a role can be edited once and every holder sees it.

`hasAccess` recomputes each time. If it is hot, cache effective permissions per user and drop the cache entry when the user, one of their groups, or one of their roles changes.

Resource hierarchy (implemented): resources are `/` paths and a grant on a folder covers its children by prefix match. `hasAccess` stays O(permissions); each check is one `startsWith`. If a user holds thousands of patterns, put them in a trie keyed by path segment and walk the requested path down it.

Time-bound grants (implemented): `assignRoleToUser(user, role, expiresAt)` stores the expiry on the user -> role link (`null` = forever). `activeRoles` ignores expired links, so access ends exactly at `expiresAt` with no background job. `purgeExpired()` actually removes them and logs `ROLE_EXPIRED`; run it on a timer. Re-granting overwrites the expiry (extend or shorten), an expiry in the past is rejected. Time comes from an injected `clock`, so tests and the demo move time by hand.

Audit log (implemented): every change records `AuditEvent(at, actor, type, subject, detail)` into an append-only `AuditLog`. Mutating calls take an `actor` (who made the change, default `system`). No-op revokes / leaves are not logged. Queries: `forSubject(user)`, `byActor(admin)`, `between(from, to, type)`. In production this goes to an append-only store (DB table with no update / delete, or a log stream), never just memory.

Nested groups (implemented): `addGroupToGroup(child, parent)` adds a parent edge. A user's groups = their direct groups plus every ancestor, found by DFS with a visited set, so a group reached by two paths is counted once. A nesting that would make a cycle is rejected up front: if `child` is already an ancestor of `parent`, adding the edge would close a loop. `groupsOf(user)` shows the expanded set.

Explicit deny (implemented): `Permission(..., Effect.DENY)`. `hasAccess` collects every permission that matches the request; one `DENY` wins over any number of `ALLOW`s, including a `*` super admin. With no match at all the answer is false (default deny). `explain(user, action, resource)` lists the role and permission behind each match, which is how you debug "why can't I see this".

Follow-ups:
- expiry on group membership too — same `Instant?` on the user -> group link
- log access checks for sensitive resources (who read payroll and when), not only grants
