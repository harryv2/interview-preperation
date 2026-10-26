const { ROUNDING_TOLERANCE } = require('../constants')

class TokenBucket {
  constructor({ capacity, refillPerWindow, windowMs, clock }) {
    this.capacity = capacity
    this.tokensPerMs = refillPerWindow / windowMs
    this.clock = clock
    this.tokens = capacity
    this.lastRefillAt = clock.now()
  }

  tryTake(cost = 1) {
    this._refill()

    if (this.tokens + ROUNDING_TOLERANCE >= cost) {
      this.tokens = Math.max(0, this.tokens - cost)
      return {
        accepted: true,
        remainingTokens: Math.floor(this.tokens + ROUNDING_TOLERANCE),
        retryAfterMs: 0,
      }
    }

    return {
      accepted: false,
      remainingTokens: Math.floor(this.tokens + ROUNDING_TOLERANCE),
      retryAfterMs: this._retryAfterMs(cost),
    }
  }

  _retryAfterMs(cost) {
    if (cost > this.capacity) {
      return null
    }

    return Math.ceil((cost - this.tokens) / this.tokensPerMs)
  }

  _refill() {
    const currentTime = this.clock.now()
    const elapsedMs = Math.max(0, currentTime - this.lastRefillAt)

    this.tokens = Math.min(this.capacity, this.tokens + elapsedMs * this.tokensPerMs)
    this.lastRefillAt = currentTime
  }
}

module.exports = { TokenBucket }
