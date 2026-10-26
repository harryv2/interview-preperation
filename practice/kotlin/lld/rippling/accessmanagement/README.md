# Access Management (RBAC)

Rippling SDE-1 LLD round. Rippling sells identity / device / app access, so this fits their domain.

- `Permission` — (action, resource pattern). Patterns:
  - `*` — any resource
  - `payroll/us/*` — everything under `payroll/us` at any depth (`payroll/us/ca`, `payroll/us/ny/2026-01`), but not `payroll/us` itself and not `payroll/usa` (the prefix keeps the trailing `/`, so it matches whole path segments)
  - `payroll/us/ca` — exactly that resource
  - a `*` anywhere else is rejected when the permission is created
- `Role` — named set of permissions
- `Group` — named set of roles (a team, a department)
- `User` — direct roles + groups
- `AccessManager` — CRUD for the above, `effectivePermissions(user)` = direct roles ∪ roles of every group, `hasAccess(user, action, resource)` checks any permission allows it

Store names, not object references, between user / group / role so a role can be edited once and every holder sees it.

`hasAccess` recomputes each time. If it is hot, cache effective permissions per user and drop the cache entry when the user, one of their groups, or one of their roles changes.

Resource hierarchy (implemented): resources are `/` paths and a grant on a folder covers its children by prefix match. `hasAccess` stays O(permissions); each check is one `startsWith`. If a user holds thousands of patterns, put them in a trie keyed by path segment and walk the requested path down it.

Follow-ups:
- explicit deny that beats allow
- nested groups — DFS over group parents with a visited set
- time-bound grants (expiresAt on the user -> role link)
- audit log of every grant / revoke
