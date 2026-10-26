# Weighted TTL Cache

Thread safe cache. Every entry has a weight (bytes, say) and a TTL. Follow-up: eviction runs asynchronously when an entry expires or the total weight goes over the limit.

- `Entry` — value, weight, expiresAt
- `entries` — `LinkedHashMap` in access order, so iteration order is least recently used first
- `totalWeight` — kept in step with every insert / remove
- one `ReentrantLock` around the map and the weight
- one daemon `ScheduledExecutorService` thread, the sweeper

Eviction:
- expiry — `get` is lazy (an expired hit is removed and returns null); the sweeper also runs every `sweepEveryMillis` so expired entries nobody reads still free their weight
- weight — `put` never blocks on eviction. If it pushed the total over the limit it hands a `cleanup` to the sweeper. Cleanup drops expired entries first, then LRU entries until the weight fits
- listeners run outside the lock so a slow listener can't stall readers

Trade-off to say out loud: async weight eviction means the cache can sit over its limit for a moment. If the limit is a hard memory budget, evict inside `put` instead (same loop, under the same lock).

Follow-ups:
- one lock is a bottleneck — shard into N caches by key hash, each with `maxWeight / N` (what Guava / Caffeine do)
- O(n) sweep — keep a min heap or a timer wheel by `expiresAt` so the sweeper only touches expired entries
- loader on miss (`getOrLoad`) with one in-flight load per key
- stats: hits, misses, evictions by reason
