# Sliding Window Rate Limiter

At most `X` calls per user in any moving `Y` second window. Follow-up: high volume must not run out of memory.

`SlidingLogRateLimiter` — exact
- per user, a deque of accepted timestamps
- drop timestamps older than the window, reject if `size >= limit`, else append
- memory O(limit) per user. At 10k calls/min per user that is 10k longs per user

`BucketedRateLimiter` — fixed memory
- the window is split into `B` buckets (ring buffer of counts plus each bucket's start time)
- a stale bucket is reset when time wraps around to it
- used = sum of buckets whose start is still inside the window
- memory O(B) per user no matter the limit. Error is at most one bucket wide (window / B), so B = 10 to 60 is usually fine
- `B = 2` with a weighted previous bucket is the classic "sliding window counter" (Cloudflare style)

Memory for many users: `evictIdle` drops users with nothing in the window. Run it on a timer, or use an LRU map with a max size.

Follow-ups: per endpoint limits (key = user + endpoint), thread safety per user instead of one lock (`ConcurrentHashMap` + lock per window), distributed (Redis sorted set for the log, `INCR` + `EXPIRE` per bucket for the counter).
