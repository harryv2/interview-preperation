# LFU Cache (LRU on ties)

Rippling OOD round (GfG on-campus report). `get` and `put` in O(1); evict the least frequently used key, and among those the least recently used.

- `entries` — key -> (value, freq)
- `keysByFreq` — freq -> `LinkedHashSet` of keys. Insertion order inside a bucket is recency, so `first()` is the LRU key at that frequency
- `minFreq` — lowest frequency that has keys. Reset to 1 on every new key. On a touch, if the old bucket empties and it was `minFreq`, bump `minFreq`

Eviction: `keysByFreq[minFreq].first()`.

Follow-ups: TTL per key, thread safety (one lock around get / put is the honest answer), generic eviction policy interface so LRU / LFU / FIFO plug in.
