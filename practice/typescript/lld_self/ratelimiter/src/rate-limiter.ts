export interface RateLimitResult {
  allowed: boolean
  limit: number
  remaining: number
  retryAfterMs: number
}

export interface RateLimiter {
  tryAcquire(key: string, limit: number, cost: number): RateLimitResult
}

export interface RateLimiterOptions {
  windowMs: number
  maxKeys?: number
}

export const DEFAULT_MAX_KEYS = 10_000
