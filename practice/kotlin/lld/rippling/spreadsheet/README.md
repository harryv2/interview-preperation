# Spreadsheet Cell Evaluator

Cells hold an integer, a string, or a formula like `=A1+B2-3`. Follow-up: dependencies, formula chains, cycle detection.

- `Formula` — parsed once on `set` into signed terms (cell ref or literal). Supports `+` and `-`
- `Spreadsheet`
  - `dependencies` — cell -> cells its formula reads
  - `dependents` — reverse edges, cell -> cells that read it
  - `cache` — computed values
- `set` — parse, reject if it makes a cycle, swap the edges, then clear the cache for the cell and everything downstream of it
- `get` — cache hit, or evaluate recursively (refs go through `get`, so shared subexpressions are computed once)

Cycle check: DFS from the new refs along `dependencies`. If it reaches the cell being set, reject. O(cells reachable). The sheet never holds a cycle, so evaluation can't loop.

Values: empty = 0 inside a formula, text inside a formula = `#VALUE` error, errors propagate.

Follow-ups:
- `*`, `/`, brackets — swap the term list for a small recursive descent parser into an AST
- `SUM(A1:A5)` — a range expands to a list of refs at parse time
- eager recompute instead of lazy — topo sort of the dirty dependents, recompute in that order
- very deep chains blow the stack — evaluate iteratively in topo order
