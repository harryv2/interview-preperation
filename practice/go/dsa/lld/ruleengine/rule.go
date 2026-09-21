package main

type Rule struct {
	ID        string
	Priority  int
	Condition Condition
	Actions   []Action
}

type Result struct {
	Matched []Rule
	Effects []Effect
}
