# Employee Grouping and Aggregation

Rippling screen. Start: total salary per department. Then: group by any field, any aggregate (sum, avg, max, count), add `filterBy`.

## Base

- `Employee` — plain data
- `Aggregator<T>` — one interface, one class per function (`Sum`, `Average`, `Max`, `Min`, `Count`). A new aggregate is a new class
- `Report<T>.groupBy(key, aggregator, filter)` — filter, group, aggregate each group. `EmployeeReport` is `Report<Employee>`

Key is a lambda, so group by one field, a computed value (age bucket), or a `Pair` for multi-level grouping without new code.

What they look for: clean use of collections, no if/else on field names, easy to add new groupings.

## Follow-ups (implemented)

**Several aggregates per group** — `groupByAll(key, aggregators)` returns `Map<K, Map<String, Double>>`, grouping once and running every aggregator on each group. Aggregators take an optional `name` (`SumAggregator("total_salary") { it.salary }`) so two sums over different fields don't collide; duplicate names are rejected.

**Nested output** — `nested(keys, aggregator)` groups by the first key, then recurses into each group with the rest. `department -> location -> headcount` is `nested(listOf({ it.department }, { it.location }), CountAggregator())`. Any depth.

**Top k groups** — `top(key, aggregator, k, descending)` sorts the group results and takes k. O(g log g) over g groups; a size-k heap makes it O(g log k) if there are many groups.

**Raw rows instead of typed objects** — the interviewer hands you a list of string maps (or CSV) and picks the columns at runtime.
- `Report` and every aggregator are generic over `T`, so `Report<Row>` (`Row = Map<String, String>`) reuses all the code above
- `Rows.parseCsv` — header line + rows, rejects rows with the wrong number of cells
- `Rows.column("department")`, `Rows.columns(listOf(...))` for multi-column keys, `Rows.number("salary")` for numeric fields
- `Rows.aggregator("avg", "salary")` builds an aggregator from strings, so a request like `?groupBy=department&agg=avg&field=salary` maps straight to a call
- unknown columns, non-numeric values and unknown aggregates fail with a clear message

## Still open

- one pass instead of group-then-aggregate — streaming aggregators with `add(item)` / `result()` (count, sum, min, max are O(1) state; avg = sum + count; median needs all values)
- `HAVING` — filter groups after aggregation (`top` is a special case)
- huge input — the same streaming aggregators, partitioned by key across workers, then merged (sum and count merge, avg merges as sum + count)
