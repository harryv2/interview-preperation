package main

import "fmt"

type Notify struct{ Message string }
type AdjustBid struct{ Percent int }
type PauseCampaign struct{}

func (n Notify) String() string      { return "Notify(" + n.Message + ")" }
func (a AdjustBid) String() string   { return fmt.Sprintf("AdjustBid(%+d%%)", a.Percent) }
func (PauseCampaign) String() string { return "PauseCampaign" }

func main() {
	rules := []Rule{
		{
			ID:        "overspend",
			Priority:  100,
			Condition: And(Gt("spend", 10_000), Not(Eq("status", "PAUSED"))),
			Actions: []Action{
				func(Facts) Effect { return PauseCampaign{} },
				func(f Facts) Effect { return Notify{fmt.Sprintf("spend %v over budget", f["spend"])} },
			},
		},
		{
			ID:        "expensive-clicks",
			Priority:  50,
			Condition: And(Gt("cpc", 10), Or(Gt("ctr", 0), Lt("views", 100))),
			Actions:   []Action{func(Facts) Effect { return AdjustBid{-10} }},
		},
		{
			ID:        "strong-performer",
			Priority:  10,
			Condition: And(Gt("ctr", 5), Lt("cpc", 2)),
			Actions:   []Action{func(Facts) Effect { return AdjustBid{20} }},
		},
	}

	allMatches := NewEngine(AllMatches{})
	firstMatch := NewEngine(FirstMatch{})
	for _, rule := range rules {
		allMatches.Add(rule)
		firstMatch.Add(rule)
	}

	campaigns := []struct {
		name  string
		facts Facts
	}{
		{"campaign-A", Facts{"spend": 12_500, "cpc": 12.5, "ctr": 1.2, "views": 5000, "status": "ACTIVE"}},
		{"campaign-B", Facts{"spend": 300, "cpc": 1.1, "ctr": 6.4, "views": 20_000, "status": "ACTIVE"}},
		{"campaign-C", Facts{"spend": 900, "cpc": 15.0, "ctr": 0.0, "views": 40, "status": "ACTIVE"}},
	}

	for _, c := range campaigns {
		all := allMatches.Evaluate(c.facts)
		first := firstMatch.Evaluate(c.facts)
		fmt.Println(c.name)
		fmt.Printf("  all matches -> %v effects %v\n", ids(all.Matched), all.Effects)
		fmt.Printf("  first match -> %v effects %v\n", ids(first.Matched), first.Effects)
	}
}

func ids(rules []Rule) []string {
	out := make([]string, 0, len(rules))
	for _, r := range rules {
		out = append(out, r.ID)
	}
	return out
}
