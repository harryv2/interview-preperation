const { parseRateLimitRules } = require('../../config/parse-rate-limit-rules')

function buildRules(rules) {
  return parseRateLimitRules({ rateLimitsPerEndpoint: rules })
}

module.exports = { buildRules }
