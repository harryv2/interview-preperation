package main

import "fmt"

type Condition interface {
	Evaluate(facts Facts) bool
}

type Operator int

const (
	GT Operator = iota
	LT
	EQ
)

func (o Operator) Test(actual, expected any) bool {
	a, aok := toFloat(actual)
	e, eok := toFloat(expected)
	switch o {
	case GT:
		return aok && eok && a > e
	case LT:
		return aok && eok && a < e
	case EQ:
		if aok && eok {
			return a == e
		}
		return actual == expected
	}
	panic(fmt.Sprintf("unknown operator %d", o))
}

type FieldCondition struct {
	Field string
	Op    Operator
	Value any
}

func (c FieldCondition) Evaluate(facts Facts) bool {
	actual, ok := facts[c.Field]
	if !ok {
		return false
	}
	return c.Op.Test(actual, c.Value)
}

type AndCondition struct {
	Children []Condition
}

func (c AndCondition) Evaluate(facts Facts) bool {
	for _, child := range c.Children {
		if !child.Evaluate(facts) {
			return false
		}
	}
	return true
}

type OrCondition struct {
	Children []Condition
}

func (c OrCondition) Evaluate(facts Facts) bool {
	for _, child := range c.Children {
		if child.Evaluate(facts) {
			return true
		}
	}
	return false
}

type NotCondition struct {
	Child Condition
}

func (c NotCondition) Evaluate(facts Facts) bool {
	return !c.Child.Evaluate(facts)
}

func Gt(field string, value float64) Condition {
	return FieldCondition{field, GT, value}
}

func Lt(field string, value float64) Condition {
	return FieldCondition{field, LT, value}
}

func Eq(field string, value any) Condition {
	return FieldCondition{field, EQ, value}
}

func And(children ...Condition) Condition {
	return AndCondition{children}
}

func Or(children ...Condition) Condition {
	return OrCondition{children}
}

func Not(child Condition) Condition {
	return NotCondition{child}
}
