const { ALGORITHMS } = require('../constants')
const { TokenBucket } = require('./token-bucket')
const { SlidingWindowCounter } = require('./sliding-window-counter')

class LimiterFactory {
  constructor(clock) {
    this.clock = clock
  }

  create(rule) {
    switch (rule.algorithm) {
      case ALGORITHMS.TOKEN_BUCKET:
        return new TokenBucket({
          capacity: rule.burst,
          refillPerWindow: rule.sustained,
          windowMs: rule.windowMs,
          clock: this.clock,
        })
      case ALGORITHMS.SLIDING_WINDOW:
        return new SlidingWindowCounter({
          limit: rule.limit,
          windowMs: rule.windowMs,
          clock: this.clock,
        })
      default:
        throw new Error(`Unknown algorithm "${rule.algorithm}"`)
    }
  }
}

module.exports = { LimiterFactory }
