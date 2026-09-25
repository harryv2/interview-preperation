# Notification Engine

Send a message to a user over a channel, with retry and a per-channel rate limit.

## Layout

- `entity/` — `Channel` (SMS / PUSH / EMAIL / WHATSAPP), `User` (address per channel), `Notification` (status, attempts, last error), `RetryPolicy` (max attempts, exponential delay), `NotificationService`
- `strategies/senders/` — `ChannelSender` and one implementation per channel
- `strategies/ratelimit/` — `RateLimiter`; `TokenBucketRateLimiter` (burst, per second), `SlidingWindowRateLimiter` (exact log of send times, O(limit) memory), `SlidingCounterRateLimiter` (approximate, two counters, previous window weighted by overlap), `PerUserRateLimiter` (one limiter per key, the service passes the user id), `CompositeRateLimiter` (all must allow)

## Flow of `send(user, channel, message)`

1. create the `Notification` (`PENDING`), hand attempt 1 to the scheduler, return a `CompletableFuture<Notification>` right away
2. an attempt takes a token from the channel's limiter (or counts as a failure) and calls the sender
3. success → `SENT`, future completes; failure → if attempts remain, `scheduler.schedule(next attempt, backoff delay)`, else `FAILED`, future completes

No thread sleeps during backoff: the scheduler holds the delayed task, the worker threads are free. In production the scheduler is a queue (SQS/Kafka) with delayed redelivery so pending notifications survive a crash.

Rate-limit hits are retried like provider errors, so a burst just spreads out over the backoff instead of being dropped. A daily-cap hit will also burn the retries and end `FAILED`; a `RATE_LIMITED` status that skips retry is the alternative.

Each channel gets `CompositeRateLimiter(burst bucket, per-user daily, channel daily)`. `tryAcquire(key)` takes the user id; channel-level limiters ignore it, `PerUserRateLimiter` uses it to pick the user's own limiter. Order matters on refusal: a wasted bucket token refills, a wasted daily slot does not, so bucket first and channel daily last.

`PerUserRateLimiter` grows one entry per user seen; in production evict idle entries (LRU or TTL) or back it with Redis.

Daily limiter choice: the exact log for hard caps (OTP per user, compliance), the counter for soft budgets (provider quota, cost) where a small over/under is fine and memory must stay constant.

## Patterns

- Strategy — `ChannelSender`, `RateLimiter`
- Decorator — `FlakySender` in the demo wraps a sender to inject failures
