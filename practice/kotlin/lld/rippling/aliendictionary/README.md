# Alien Dictionary

Reported on Glassdoor: given words sorted in an unknown alphabet, return a valid letter order.

1. every letter is a node
2. for each adjacent pair, the first differing letter gives an edge `a -> b`
3. if there's no differing letter and the first word is longer (`abc` before `ab`), the input is invalid
4. Kahn's topological sort. If not every letter comes out, there is a cycle, return `""`

O(total characters + edges). Sorting the queue only makes the output deterministic.

Follow-ups: return all valid orders (backtracking over the zero in-degree set), check whether the order is unique (queue never holds more than one letter), verify a given list is sorted for a known order.
