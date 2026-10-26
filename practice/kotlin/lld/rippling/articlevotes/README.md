# Article Like / Dislike

Rippling screen (Glassdoor). Users like or dislike articles. Get like and dislike counts per article, and the last 3 times a user switched a vote. Every operation O(1).

- `votes` — (user, article) -> current vote
- `counts` — article -> likes, dislikes. Updated on every change so reads are O(1)
- `switches` — user -> deque of the last 3 `VoteSwitch(article, from, to)`

Rules: same vote twice is a no-op, a different vote moves one count from one side to the other and records a switch, `clear` removes the vote without recording a switch.

Follow-ups: top k liked articles (TreeSet keyed on likes, reinsert on change), score = likes − dislikes, thread safety (per article lock or `ConcurrentHashMap` + `AtomicInteger`).
