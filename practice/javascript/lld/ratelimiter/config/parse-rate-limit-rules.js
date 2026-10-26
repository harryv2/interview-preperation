const { configSchema } = require('../utils/joi-schemas')

function parseRateLimitRules(rateLimitConfig) {
  const { value, error } = configSchema.validate(rateLimitConfig, { abortEarly: false })

  if (error) {
    throw new Error(`Invalid config: ${error.message}`)
  }

  return value.rateLimitsPerEndpoint
}

module.exports = { parseRateLimitRules }
