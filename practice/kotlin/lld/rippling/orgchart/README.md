# Org Chart Analyzer

Reporting tree. APIs: common manager, reporting line between two people, indirect report count. Follow-up: restructuring (move a manager with or without the team) without orphans or deadlocks.

- `Employee` — id, name, department, `manager` pointer up, `reports` set down
- `OrgChart` — id -> employee, one read/write lock

APIs:
- `commonManager(a, b)` — put a's chain to the root in a set, walk b's chain up until it hits the set. O(depth). Includes the people themselves, so if `a` manages `b` the answer is `a`
- `reportingLine(a, b)` — a up to the common manager, then down to b
- `totalReportCount` — size of the subtree minus self. `indirect = total - direct`
- `move(id, newManager, withTeam, newDepartment)`
  - with team: reparent one node, the subtree comes along
  - without team: the person's reports go to their old manager first, then the person moves alone
  - rejects a move under someone in their own subtree (that would make a cycle and detach the branch from the root)
- `remove(id)` — reports go up to the removed person's manager, so nobody is orphaned

Deadlocks: there's one lock for the whole chart, so there is no lock ordering to get wrong. A move is a single write section, so no reader sees a half moved team. Reads (the common case) run in parallel.

Follow-ups:
- counts are O(subtree) — keep a `subtreeSize` per node and update the ancestor chain on add / move / remove (O(depth))
- many LCA queries on a static tree — binary lifting, O(log n) per query
- matrix orgs (two managers) — the tree becomes a DAG; LCA becomes "closest common ancestor" by BFS from both sides
- per department locking — only safe if every move locks both departments in a fixed order (sorted by name)
