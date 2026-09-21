package main

type SelectionStrategy interface {
	Select(candidates []Rule, facts Facts) []Rule
}

type AllMatches struct{}

func (AllMatches) Select(candidates []Rule, facts Facts) []Rule {
	var selected []Rule
	for _, rule := range candidates {
		if rule.Condition.Evaluate(facts) {
			selected = append(selected, rule)
		}
	}
	return selected
}

type FirstMatch struct{}

func (FirstMatch) Select(candidates []Rule, facts Facts) []Rule {
	for _, rule := range candidates {
		if rule.Condition.Evaluate(facts) {
			return []Rule{rule}
		}
	}
	return nil
}
