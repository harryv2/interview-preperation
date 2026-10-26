# Key Value Store with Transactions

Most reported Rippling phone screen. `get / set / delete`, then `begin / commit / rollback`, then nested transactions.

- `data` — the committed map
- `transactions` — a stack of overlay maps, one per open `begin`. A deleted key is stored as `null` (tombstone) so it hides the committed value
- `get` — walk the stack from the top, first layer that has the key wins, else `data`
- `commit` — merge the top layer into its parent, or into `data` if it is the outermost
- `rollback` — pop the top layer

Writes are O(1), `get` is O(depth). Commit is O(keys touched in that layer).

Follow-ups: `count(value)` (keep a value -> count map per layer, or recompute on read), TTL per key, thread safety (one lock, or a transaction per thread via ThreadLocal stack).
