# Expense Rules Engine

Rippling corporate card screen. Given expenses and rules, return every rule each expense (and each trip) violates.

Sample rules from reports: no expense over 175, restaurant expenses over 45 not allowed, entertainment not allowed, trip total under some limit, meals per trip under some limit.

## Base

- `Expense` — id, tripId, employeeId, department, amount, expenseType, vendorType, vendorName, date
- `Violation` — rule name, the expense or group id, a message, severity
- `ExpenseRule` — checks one expense
- `GroupRule` — checks all expenses of one group (aggregate rules need the grouping step)
- `RulesEngine` — runs every expense rule on every expense, groups expenses, runs every group rule on every group, collects all violations (no short circuit)

The point the interviewer pushes on: a new rule is a new class, the engine does not change. No if/else chain on rule type.

## Follow-ups (implemented)

**Per employee / per department rules** — `GroupBy` enum holds the key selector (`TRIP`, `EMPLOYEE`, `DEPARTMENT`). A group rule declares its `groupBy`; the engine groups the expenses once per distinct key and runs the matching rules. `GroupTotalLimitRule(GroupBy.EMPLOYEE, 300)` is the same class as the trip version. A new grouping = one enum value.

**Composite rules** — `AllOfRule` fires only if every child fires ("bar over 100 AND vendor is Lucky Bar"). `AnyOfRule` fires if any child fires (several bans under one name). Both are `ExpenseRule`s, so they nest.

**Severity** — `WARN` or `BLOCK` on every rule, copied onto its violation. `Report.blocked` is true if any `BLOCK` violation exists; warnings show but don't stop the expense.

**Rules from config** — `RuleFactory.fromConfig(list of maps)` maps `{"type": ..., params, "severity"?}` to rule objects; `groupBy` present means a group rule, `all_of` / `any_of` take nested `rules`. This is the JSON a rules admin page would save. Unknown types or missing params fail when the config is loaded, not on the 1000th expense.

```
{"type": "vendor_type_limit", "vendorType": "restaurant", "limit": "45", "severity": "warn"}
{"type": "all_of", "name": "big_bar_tab", "rules": [ {...}, {...} ]}
{"type": "total_limit", "groupBy": "employee", "limit": "300"}
```

Adding a rule type now = one class + one line in the factory.

## More follow-ups (implemented, in `followup/`)

**Time window groups** — `GroupBy.EMPLOYEE_MONTH` keys on `employeeId:yyyy-MM`. "Max 150 per employee per month" is `{"type": "total_limit", "groupBy": "employee_month", "limit": "150"}`, no new rule class.

**Rule versioning** — `VersionedRuleBook` keeps a `TreeMap<effectiveFrom, RulesEngine>`. Each expense is checked against `floorKey(expense.date)`, the rule set live on the day it was spent, so changing a limit today doesn't flag last month's expenses. Group rules run per version batch, so a trip that spans a rule change is split at the boundary (call that out).

**Incremental / streaming** — `IncrementalRulesEngine.add(expense)` checks only that expense and only the groups it belongs to, and returns just the violations it newly raised. `open` holds current violations keyed by (rule, target), so a trip already over its limit isn't reported again on every new expense. Re-checking a group is O(group size); for O(1), keep a running total per (rule, group) and compare it to the limit.

**Rule index** — an expense rule may declare a `Trigger(field, value)` (vendor type = restaurant, expense type = entertainment). `RuleIndex` buckets rules by trigger; `candidates(expense)` returns the rules with no trigger plus one map lookup per field. With 1000s of vendor- or category-specific rules each expense runs only a handful. `RulesEngine` uses the index too.

## Still open

- a rule that changes after expenses were approved — re-run only the affected groups and diff against the stored violations
- explain why a composite fired — return the child violations, not just the joined message
- concurrency for the incremental engine — lock per group key, or partition the stream by employee
