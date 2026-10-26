# Extensible Policy Engine (Composite)

"If amount > 500 AND department is Marketing, require VP approval." Follow-up: non-engineers add nested `AND / OR / NOT` rules without code changes.

The senior version of [expenserules](../expenserules/README.md). That one has one class per rule; this one builds rules out of small parts.

- `Condition` — `test(facts)`. Composite pattern:
  - leaf `FieldCondition(field, op, value)` with `EQ NEQ GT GTE LT LTE IN`
  - `AndCondition`, `OrCondition`, `NotCondition` hold children, so any depth works
- `ConditionParser` — turns a JSON-shaped map into the tree:
  `{"and": [...]}`, `{"or": [...]}`, `{"not": {...}}`, `{"field", "op", "value"}`. This is what a rules UI or a config file saves
- `Policy` — name + condition + action
- `PolicyEngine.evaluate(facts)` — every matching policy, decision = strongest action (`REJECT > VP > MANAGER > ALLOW`, enum order)

Facts are a plain `Map<String, Any>`, so a new field (cost center, merchant country) needs no code, only a new rule.

Adding an operator = one enum value + one `when` branch. Adding a new node type (e.g. `AtLeast(n, children)`) = one class + one parser key.

Follow-ups:
- validate config on save (unknown field, wrong type for `GT`) so bad rules fail early, not on the 1000th expense
- rule versioning — store rules with `effectiveFrom`, evaluate with the version live at expense time
- explain why — return the leaf conditions that made it true
- performance with thousands of rules — index rules by a required leaf (e.g. department) and only evaluate candidates
