# Access Management (RBAC)

Rippling SDE-1 LLD round. Rippling sells identity / device / app access, so this fits their domain.

- `Permission` — (action, resource). Resource `*` matches any resource
- `Role` — named set of permissions
- `Group` — named set of roles (a team, a department)
- `User` — direct roles + groups
- `AccessManager` — CRUD for the above, `effectivePermissions(user)` = direct roles ∪ roles of every group, `hasAccess(user, action, resource)` checks any permission allows it

Store names, not object references, between user / group / role so a role can be edited once and every holder sees it.

`hasAccess` recomputes each time. If it is hot, cache effective permissions per user and drop the cache entry when the user, one of their groups, or one of their roles changes.

Follow-ups:
- resource hierarchy (`payroll/us/*`) — match by prefix
- explicit deny that beats allow
- nested groups — DFS over group parents with a visited set
- time-bound grants (expiresAt on the user -> role link)
- audit log of every grant / revoke
