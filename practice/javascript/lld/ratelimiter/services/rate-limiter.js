const LRU = require('lru-cache')
const { DEFAULT_MAX_LIMITERS } = require('../constants')

class RateLimiter {
  constructor(rateLimitRules, limiterFactory, appConfig = { maxLimiters: DEFAULT_MAX_LIMITERS }) {
    this.rulesByEndpoint = new Map(rateLimitRules.map((rule) => [rule.endpoint, rule]))
    this.limiterFactory = limiterFactory
    this.limiters = new LRU({ max: appConfig.maxLimiters })
  }

  hasEndpoint(endpoint) {
    return this.rulesByEndpoint.has(endpoint)
  }

  take(endpoint, clientId) {
    const rule = this.rulesByEndpoint.get(endpoint)
    if (!rule) {
      throw new Error(`No rate limit configured for "${endpoint}"`)
    }

    const limiterKey = JSON.stringify([endpoint, clientId || null])

    let limiter = this.limiters.get(limiterKey)
    if (!limiter) {
      limiter = this.limiterFactory.create(rule)
      this.limiters.set(limiterKey, limiter)
    }

    return limiter.tryTake()
  }
}

module.exports = { RateLimiter }
