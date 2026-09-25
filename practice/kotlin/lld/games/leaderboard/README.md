Real Time Gaming Leaderboard

- Millions of players, scores changing constantly, ranks asked for on every screen
- Submit a score and the board reorders immediately
- Read the top N, read one player's rank, read the slice around one player
- Boards are per game and per window, daily, weekly and all time
- How a score merges is a game rule: personal best, latest run, or cumulative
- The in memory board is the read path, the relational table is the durable copy

Entities

ScoreEntry, RankedEntry
LeaderboardKey (gameId, period, bucket)
ScoreBoard, ScoreRecord

The data structure, which is the whole problem

A Redis ZSET is two things at once:

  hash      member -> score        O(1) "what is this player on"
  skiplist  ordered by score       O(log n) insert, delete, rank, select by rank

ScoreBoard is the same pair: a HashMap from playerId to the entry, and a RankedSet of entries. The hash is what
makes an update cheap. Without it, moving a player means finding them first, and finding them in an ordered
structure keyed by score means a scan.

    submit -> hash gives the old entry in O(1)
           -> remove it from the ordered set  O(log n)
           -> insert the new one              O(log n)

The trap worth knowing

    TreeSet<ScoreEntry> set = new TreeSet<>(byScoreDesc);
    set.headSet(entry).size()          // this is O(n), not O(log n)

java.util.TreeSet and TreeMap are red black trees that store no subtree sizes. They can answer "what is next"
in O(log n) and they cannot answer "what position is this" in anything better than O(n), because size() on a
headSet view walks the view. Same for reading the 5000th element: there is no way in except from an end.

Redis solves it by hanging a span on every forward pointer of every skiplist node, the count of nodes that
pointer steps over. Accumulate spans on the way down and you have the rank.

OrderStatisticTree here solves it the same way with one field instead: every node carries its subtree size.

    rank(v)     descend, add left subtree size + 1 each time you go right    O(log n)
    select(i)   descend, compare i against the left subtree size             O(log n)
    range(f,c)  descend once, then walk only the subtrees the window touches O(log n + c)

It is a treap, so the balance is probabilistic rather than a skiplist's level coin flips. Either is a fine
answer to "implement ZRANK", the augmentation is the point and the balancing scheme is not.

Both implementations sit behind RankedSet, and Demo builds 100k entries into each to show the gap.

Database indexing, the fallback path

    CREATE TABLE score (
        game_id     VARCHAR(32) NOT NULL,
        period      VARCHAR(16) NOT NULL,
        bucket      VARCHAR(16) NOT NULL,
        player_id   VARCHAR(64) NOT NULL,
        score       BIGINT      NOT NULL,
        achieved_at TIMESTAMP   NOT NULL,
        PRIMARY KEY (game_id, period, bucket, player_id)
    );

    CREATE INDEX idx_board_rank
        ON score (game_id, period, bucket, score DESC, achieved_at ASC, player_id);

- The primary key answers "what is this player on" as one point lookup, no second index needed for it
- The secondary index leads with the three board columns, so one board is a contiguous run inside the index
- score DESC matches the display order, so top N is a range scan and the database never sorts
- achieved_at and player_id trail it to make the order total, which is what makes keyset pagination stable
- It covers the leaderboard page, every column the page renders is in the index, so there are no heap lookups
- Page by keyset, not by offset:
      WHERE (game_id, period, bucket) = (?, ?, ?)
        AND (score, achieved_at, player_id) < (?, ?, ?)
      ORDER BY score DESC, achieved_at, player_id
      LIMIT 50
  OFFSET 10000 costs O(offset) and gets worse the deeper anyone pages, a keyset seek does not

What the index still cannot do is give you a rank. RANK() OVER (PARTITION BY game_id, period, bucket
ORDER BY score DESC) reads the whole partition, and a covering index only makes that scan sequential rather
than random. It is still O(n) per query. That is the reason the ordered set exists, and the reason nobody
serves "you are #4,182,003" out of SQL. If it has to come from SQL, materialise a rank column on a schedule
and accept that it is stale between runs.

Decisions worth defending

- The hash and the ordered set are one unit, neither is useful alone, and keeping them in sync is why they sit
  behind one class rather than being two fields on a service
- Ties break on who got there first, not on player id, and a personal best that is not beaten keeps its
  original timestamp so a resubmission cannot quietly steal a rank
- The bucket in the key is what makes a daily board a separate board, and lets an expired day be dropped whole
  rather than filtered out of a shared structure on every read
- One submission fans out into every window it belongs to, each board stays independent and none of them needs
  to know about the others
- The repository has no rankOf on purpose, the absence is the design statement
- A board missing from memory is rebuilt from the durable copy before it serves anything, so a restart is a
  latency event and not a correctness one

Swappable
- score policy (personal best, latest, cumulative)
- the ordered structure behind RankedSet, which is how the TreeSet version can be benchmarked against the real one

Not modelled
- sharding a board across nodes, and the fact that an exact global rank stops being cheap or meaningful once
  the board is sharded, real systems keep exact ranks for the top K and percentile buckets for the long tail
- replication and failover, write batching to the durable store, hot key contention on a single popular board
- anti cheat and score validation, seasons and resets, friend and regional boards, ties shown as shared ranks
