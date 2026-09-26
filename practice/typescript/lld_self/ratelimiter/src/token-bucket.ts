import { LruTtlCache } from "./lru-ttl-cache"
import { DEFAULT_MAX_KEYS, RateLimiter, RateLimiterOptions, RateLimitResult } from "./rate-limiter"

type Bucket = {
  tokens: number
  lastRefillAt: number
}

export class TokenBucket implements RateLimiter {
  private readonly windowMs: number
  private readonly buckets: LruTtlCache<string, Bucket>

  constructor({ windowMs, maxKeys = DEFAULT_MAX_KEYS }: RateLimiterOptions) {
    if (windowMs <= 0) {
      throw new Error("windowMs must be > 0")
    }

    this.windowMs = windowMs
    this.buckets = new LruTtlCache({ maxEntries: maxKeys, ttlMs: windowMs })
  }

  tryAcquire(key: string, limit: number, cost: number): RateLimitResult {
    const now = Date.now()
    const tokensPerMs = limit / this.windowMs

    let bucket = this.buckets.get(key)
    if (!bucket) {
      bucket = { tokens: limit, lastRefillAt: now }
      this.buckets.set(key, bucket)
    }

    const elapsedMs = Math.max(0, now - bucket.lastRefillAt)
    bucket.tokens = Math.min(limit, bucket.tokens + elapsedMs * tokensPerMs)
    bucket.lastRefillAt = now

    if (bucket.tokens >= cost) {
      bucket.tokens -= cost
      return {
        allowed: true,
        limit,
        remaining: Math.floor(bucket.tokens),
        retryAfterMs: 0,
      }
    }

    return {
      allowed: false,
      limit,
      remaining: Math.floor(bucket.tokens),
      retryAfterMs: this.retryAfterMs(bucket.tokens, limit, cost, tokensPerMs),
    }
  }

  private retryAfterMs(tokens: number, limit: number, cost: number, tokensPerMs: number): number {
    if (cost > limit) {
      return Infinity
    }

    const missingTokens = cost - tokens
    return Math.ceil(missingTokens / tokensPerMs)
  }
}
