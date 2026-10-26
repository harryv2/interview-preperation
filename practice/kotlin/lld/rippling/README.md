# Rippling

Coding / LLD questions reported for Rippling (LeetCode discuss, Glassdoor, 1point3acres, PracHub, Blind, Substack write-ups). Each one is kept small enough to finish in about an hour, with the usual follow-ups listed in its README.

Most asked first:

| folder | question | usual follow-up |
|---|---|---|
| kvstore | key value store `get / set / delete` | `begin / commit / rollback`, nested transactions |
| deliverycost | drivers with hourly rate, record deliveries, total cost | `payUpTo(time)`, unpaid cost |
| musicanalytics | songs, plays, analytics by unique listeners | last 3 songs per user, star / unstar |
| expenserules | run rules over expenses, return all violations | per trip / employee / department rules, AND / OR composites, warn vs block, rules from config |
| employeegrouping | total salary per department | any key, any aggregate, several aggregates at once, nested output, top k, raw CSV rows |
| articlevotes | like / dislike counts per article | last 3 vote switches per user, all O(1) |
| canvas | 2D grid, draw and move rectangles | out of bounds, infinite grid |
| lfucache | LFU cache, LRU on ties | O(1) everything |
| accessmanagement | users, groups, roles, permissions | wildcards, deny, nested groups |

Senior level (LLD + DSA mixed, concurrency follow-ups). From a shared prep list. No candidate reports found for spreadsheet, org chart, task runner or cron, so treat those as practice, not confirmed.

| folder | question | follow-up covered |
|---|---|---|
| spreadsheet | cells with ints, strings, `=A1+B2` formulas | dependency graph, cache invalidation, cycle rejection |
| mediansorted | median of two sorted salary arrays | N arrays (binary search on value), N streams (k-way merge), running median |
| ratelimiter | X calls per Y seconds per user, sliding | fixed memory bucketed counter, idle user eviction |
| policyengine | "amount > 500 AND dept = Marketing -> VP approval" | composite AND / OR / NOT built from JSON-shaped config |
| rbachierarchy | multi-tenant RBAC, resource tree, nested groups | cached principal set, versioned invalidation, RW lock |
| orgchart | common manager, reporting line, indirect reports | move with or without team, remove without orphans, one lock |
| kvstore | transactional KV store | nested transactions (already above) |
| weightedcache | thread safe cache with weight + TTL | async eviction on expiry or weight breach, LRU |
| prioritytaskrunner | async tasks with HIGH / MEDIUM / LOW | K workers, aging so LOW never starves |
| cronscheduler | run jobs every N ms | overlap policy when a run is longer than its interval |
| aliendictionary | letter order from a sorted alien word list | topo sort, invalid prefix, cycle (reported on Glassdoor) |

Rounds also reported but not here because they are not LLD: LeetCode medium / hard DSA, a React form round, an API integration round (call given APIs to compute a result), and HLD for senior levels.
