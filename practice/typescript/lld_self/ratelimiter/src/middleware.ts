import { NextFunction, Request, RequestHandler, Response } from "express"
import { RateLimiter } from "./rate-limiter"
import { SlidingCounter } from "./sliding-counter"
import { TokenBucket } from "./token-bucket"

type ValueOrFromRequest<T> = T | ((req: Request) => T)

export type RateLimitAlgorithm = "token-bucket" | "sliding-counter"

export interface RateLimitOptions {
  algorithm: RateLimitAlgorithm
  limit: ValueOrFromRequest<number>
  windowMs: number
  key?: (req: Request) => string
  cost?: ValueOrFromRequest<number>
  skip?: (req: Request) => boolean
  maxKeys?: number
}

const clientIp = (req: Request) => req.ip ?? "unknown"

export function rateLimit(options: RateLimitOptions): RequestHandler {
  const { limit, key = clientIp, cost = 1, skip } = options

  validateOptions(options)
  const limiter = createLimiter(options)

  return (req: Request, res: Response, next: NextFunction) => {
    if (skip?.(req)) {
      return next()
    }

    const requestLimit = resolveValue(limit, req)
    const requestCost = resolveValue(cost, req)

    if (!isPositiveNumber(requestLimit) || !isPositiveNumber(requestCost)) {
      return next(new Error(`Invalid rate limit: limit=${requestLimit}, cost=${requestCost}`))
    }

    const result = limiter.tryAcquire(key(req), requestLimit, requestCost)

    res.setHeader("RateLimit-Limit", result.limit)
    res.setHeader("RateLimit-Remaining", Math.max(0, result.remaining))

    if (result.allowed) {
      return next()
    }

    if (Number.isFinite(result.retryAfterMs)) {
      res.setHeader("Retry-After", Math.ceil(result.retryAfterMs / 1000))
    }

    res.status(429).json({ error: "Too many requests" })
  }
}

function createLimiter({ algorithm, windowMs, maxKeys }: RateLimitOptions): RateLimiter {
  switch (algorithm) {
    case "token-bucket":
      return new TokenBucket({ windowMs, maxKeys })
    case "sliding-counter":
      return new SlidingCounter({ windowMs, maxKeys })
  }
}

function validateOptions({ limit, windowMs, cost, maxKeys }: RateLimitOptions): void {
  assertPositive("windowMs", windowMs)

  if (typeof limit === "number") {
    assertPositive("limit", limit)
  }
  if (typeof cost === "number") {
    assertPositive("cost", cost)
  }
  if (maxKeys !== undefined) {
    assertPositive("maxKeys", maxKeys)
  }
}

function assertPositive(name: string, value: number): void {
  if (!isPositiveNumber(value)) {
    throw new Error(`${name} must be a positive number, got ${value}`)
  }
}

function isPositiveNumber(value: number): boolean {
  return Number.isFinite(value) && value > 0
}

function resolveValue<T>(value: ValueOrFromRequest<T>, req: Request): T {
  if (typeof value === "function") {
    return (value as (req: Request) => T)(req)
  }
  return value
}
