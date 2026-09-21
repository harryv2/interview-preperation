package main

import "fmt"

type Effect fmt.Stringer

type Action func(facts Facts) Effect
