# Rule Engine

Rules are `condition -> actions`. The engine evaluates a `Facts` bag against all rules in priority order and returns the effects to apply.

## Layout

- `entity/` — `Facts`, `Rule`, `RuleResult`, `RuleEngine`
- `strategies/conditions/` — `Condition` tree: `FieldCondition` leaf, `And` / `Or` / `Not` composites, `Operator`, and `OperatorHelpers` (`"cpc" gt 10`, `a and b`)
- `strategies/actions/` — `Action` (facts -> `Effect`), `Effect` marker
- `strategies/selection/` — `RuleSelectionStrategy`: which matching rules run. `AllMatchesStrategy`, `FirstMatchStrategy`

## Flow

1. `RuleEngine(selectionStrategy)`; `engine.add(Rule(id, priority, condition, actions))`
2. `engine.evaluate(facts)` — sorts by priority, asks the selection strategy which rules match, runs their actions
3. `RuleResult(matched, effects)` — caller applies the effects

`FirstMatchStrategy` stops at the highest-priority match; `AllMatchesStrategy` runs every match.

## Patterns

- Composite — nested conditions of any depth
- Strategy — `RuleSelectionStrategy`, `Action`, `Condition`
- Specification — `Condition` is data (`field, op, value`), so rules can be loaded from JSON
