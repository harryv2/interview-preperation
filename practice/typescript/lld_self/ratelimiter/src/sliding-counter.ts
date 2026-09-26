import { LruTtlCache } from "./lru-ttl-cache"
import { DEFAULT_MAX_KEYS, RateLimiter, RateLimiterOptions, RateLimitResult } from "./rate-limiter"

type WindowCounts = {
  windowStart: number
  previousCount: number
  currentCount: number
}

export class SlidingCounter implements RateLimiter {
  private readonly windowMs: number
  private readonly windows: LruTtlCache<string, WindowCounts>

  constructor({ windowMs, maxKeys = DEFAULT_MAX_KEYS }: RateLimiterOptions) {
    if (windowMs <= 0) {
      throw new Error("windowMs must be > 0")
    }

    this.windowMs = windowMs
    this.windows = new LruTtlCache({ maxEntries: maxKeys, ttlMs: 2 * windowMs })
  }

  tryAcquire(key: string, limit: number, cost: number): RateLimitResult {
    const now = Date.now()
    const currentWindowStart = Math.floor(now / this.windowMs) * this.windowMs

    let counts = this.windows.get(key)
    if (!counts) {
      counts = { windowStart: currentWindowStart, previousCount: 0, currentCount: 0 }
      this.windows.set(key, counts)
    }

    this.rollWindow(counts, currentWindowStart)

    const elapsedInWindowMs = now - currentWindowStart
    const previousWeight = 1 - elapsedInWindowMs / this.windowMs
    const used = counts.previousCount * previousWeight + counts.currentCount

    if (used + cost <= limit) {
      counts.currentCount += cost
      return {
        allowed: true,
        limit,
        remaining: Math.floor(limit - used - cost),
        retryAfterMs: 0,
      }
    }

    return {
      allowed: false,
      limit,
      remaining: Math.max(0, Math.floor(limit - used)),
      retryAfterMs: this.retryAfterMs(counts, now, limit, cost),
    }
  }

  private rollWindow(counts: WindowCounts, currentWindowStart: number): void {
    const windowsPassed = (currentWindowStart - counts.windowStart) / this.windowMs

    if (windowsPassed >= 2) {
      counts.previousCount = 0
      counts.currentCount = 0
    } else if (windowsPassed === 1) {
      counts.previousCount = counts.currentCount
      counts.currentCount = 0
    }

    counts.windowStart = currentWindowStart
  }

  // Previous window's weight falls linearly to 0 over the current window.
  // If the request fits once that weight drops enough, wait inside this window.
  // Otherwise wait for the next window, where currentCount becomes the decaying part.
  private retryAfterMs(counts: WindowCounts, now: number, limit: number, cost: number): number {
    if (cost > limit) {
      return Infinity
    }

    if (counts.currentCount + cost <= limit) {
      const requiredElapsedRatio = 1 - (limit - counts.currentCount - cost) / counts.previousCount
      const allowedAt = counts.windowStart + requiredElapsedRatio * this.windowMs
      return Math.max(0, Math.ceil(allowedAt - now))
    }

    const nextWindowStart = counts.windowStart + this.windowMs
    const requiredElapsedRatio = 1 - (limit - cost) / counts.currentCount
    const allowedAt = nextWindowStart + requiredElapsedRatio * this.windowMs
    return Math.max(0, Math.ceil(allowedAt - now))
  }
}
