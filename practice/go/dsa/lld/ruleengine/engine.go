package main

import (
	"cmp"
	"fmt"
	"slices"
	"sync"
)

type Engine struct {
	selection SelectionStrategy

	mu    sync.RWMutex
	rules []Rule
}

func NewEngine(selection SelectionStrategy) *Engine {
	return &Engine{selection: selection}
}

func (e *Engine) Add(rule Rule) error {
	e.mu.Lock()
	defer e.mu.Unlock()

	if slices.ContainsFunc(e.rules, func(r Rule) bool { return r.ID == rule.ID }) {
		return fmt.Errorf("rule %q already exists", rule.ID)
	}
	e.rules = append(e.rules, rule)
	return nil
}

func (e *Engine) Remove(id string) {
	e.mu.Lock()
	defer e.mu.Unlock()
	e.rules = slices.DeleteFunc(e.rules, func(r Rule) bool { return r.ID == id })
}

func (e *Engine) Evaluate(facts Facts) Result {
	e.mu.RLock()
	byPriority := slices.Clone(e.rules)
	e.mu.RUnlock()

	slices.SortFunc(byPriority, func(a, b Rule) int { return cmp.Compare(b.Priority, a.Priority) })
	selected := e.selection.Select(byPriority, facts)

	var effects []Effect
	for _, rule := range selected {
		for _, action := range rule.Actions {
			effects = append(effects, action(facts))
		}
	}
	return Result{Matched: selected, Effects: effects}
}
