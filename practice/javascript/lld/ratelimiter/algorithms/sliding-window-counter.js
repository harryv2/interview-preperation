const { ROUNDING_TOLERANCE } = require('../constants')

class SlidingWindowCounter {
  constructor({ limit, windowMs, clock }) {
    this.limit = limit
    this.windowMs = windowMs
    this.clock = clock
    this.windowStart = this._windowStartFor(clock.now())
    this.previousCount = 0
    this.currentCount = 0
  }

  tryTake(cost = 1) {
    const currentTime = Math.max(this.clock.now(), this.windowStart)
    this._rollWindow(currentTime)

    const previousWeight = 1 - (currentTime - this.windowStart) / this.windowMs
    const used = this.previousCount * previousWeight + this.currentCount

    if (used + cost <= this.limit + ROUNDING_TOLERANCE) {
      this.currentCount += cost
      return {
        accepted: true,
        remainingTokens: Math.max(0, Math.floor(this.limit - used - cost + ROUNDING_TOLERANCE)),
        retryAfterMs: 0,
      }
    }

    return {
      accepted: false,
      remainingTokens: Math.max(0, Math.floor(this.limit - used + ROUNDING_TOLERANCE)),
      retryAfterMs: this._retryAfterMs(currentTime, cost),
    }
  }

  _windowStartFor(time) {
    return Math.floor(time / this.windowMs) * this.windowMs
  }

  _rollWindow(currentTime) {
    const currentWindowStart = this._windowStartFor(currentTime)
    const windowsPassed = Math.round((currentWindowStart - this.windowStart) / this.windowMs)

    if (windowsPassed >= 2) {
      this.previousCount = 0
      this.currentCount = 0
    } else if (windowsPassed === 1) {
      this.previousCount = this.currentCount
      this.currentCount = 0
    }

    this.windowStart = currentWindowStart
  }

  // The previous window's weight falls linearly to 0 across the current window.
  // If the request fits once that weight is low enough, it frees up in this window.
  // Otherwise it waits for the next window, where currentCount becomes the decaying part.
  _retryAfterMs(currentTime, cost) {
    if (cost > this.limit) {
      return null
    }

    let allowedAt

    if (this.currentCount + cost <= this.limit) {
      const requiredElapsedRatio = 1 - (this.limit - this.currentCount - cost) / this.previousCount
      allowedAt = this.windowStart + requiredElapsedRatio * this.windowMs
    } else {
      const requiredElapsedRatio = 1 - (this.limit - cost) / this.currentCount
      allowedAt = this.windowStart + this.windowMs + requiredElapsedRatio * this.windowMs
    }

    return Math.max(0, Math.ceil(allowedAt - currentTime))
  }
}

module.exports = { SlidingWindowCounter }
