const appConfig = require('../config/app-config')
const defaultRateLimitConfig = require('../config.json')
const { parseRateLimitRules } = require('../config/parse-rate-limit-rules')
const { SystemClock } = require('../utils/clock')
const { LimiterFactory } = require('../algorithms/limiter-factory')
const { RateLimiter } = require('../services/rate-limiter')
const { TakeApi } = require('../api/take-api')

function buildContainer(overrides = {}) {
  const clock = overrides.clock || new SystemClock()
  const rateLimitConfig = overrides.rateLimitConfig || defaultRateLimitConfig

  const rateLimitRules = parseRateLimitRules(rateLimitConfig)
  const limiterFactory = new LimiterFactory(clock)
  const rateLimiter = overrides.rateLimiter || new RateLimiter(rateLimitRules, limiterFactory, appConfig)
  const takeApi = new TakeApi(rateLimiter)

  return { appConfig, rateLimiter, takeApi }
}

module.exports = { buildContainer }
