# Multi-Tenant RBAC with Inheritance

Users, nested groups, roles, a resource tree, per tenant. A grant on a parent resource covers its children. Follow-up: fast `check` when a user sits in dozens of overlapping groups over deep hierarchies.

The senior version of [accessmanagement](../accessmanagement/README.md).

- `Tenant` — everything is scoped to one tenant, so tenants can't see each other's data
  - `roles` — role -> permissions
  - `resources` — id -> parent id (a tree: root / hr / payroll / payroll-us)
  - `groupParents` — group -> groups it belongs to (a DAG, cycles rejected)
  - `userGroups` — user -> direct groups
  - `grants` — resource id -> (principal, role). A principal is a user or a group
- `check(tenant, user, permission, resource)`
  1. principals = user + every ancestor group (BFS). Cached per user
  2. walk from the resource up to the root; at each node look at its grants and return on the first grant whose principal is in the set and whose role has the permission

Cost: principal set is O(groups reachable) once, then O(1) membership. The walk is O(depth × grants on each node on the path). Grants per node are usually few, so this beats "expand every group into every resource".

Cache invalidation: every write bumps `tenant.version`; a cached principal set is only used if its version matches. Simple and correct; coarse (any change in the tenant drops every cache entry for it).

Concurrency: one read/write lock. Checks run in parallel, writes are rare and exclusive.

Follow-ups:
- explicit deny that beats allow — collect allows and denies on the walk, deny wins
- faster at scale — cache the decision per (user, resource, permission) with the same version trick, or precompute a closure table of resource ancestors
- finer invalidation — version per user and per group instead of per tenant
- audit — log every check that returns true for sensitive resources
