const Joi = require('joi')

const {
  ALGORITHMS,
  DEFAULT_ALGORITHM,
  DEFAULT_WINDOW_MS,
  MAX_CLIENT_ID_LENGTH,
} = require('../constants')

function requiredFor(algorithm, schema) {
  return Joi.when('algorithm', { is: algorithm, then: schema.required(), otherwise: Joi.forbidden() })
}

const ruleSchema = Joi.object({
  endpoint: Joi.string().trim().min(1).required(),
  algorithm: Joi.string().valid(...Object.values(ALGORITHMS)).default(DEFAULT_ALGORITHM),
  windowMs: Joi.number().integer().min(1).default(DEFAULT_WINDOW_MS),
  burst: requiredFor(ALGORITHMS.TOKEN_BUCKET, Joi.number().integer().min(1)),
  sustained: requiredFor(ALGORITHMS.TOKEN_BUCKET, Joi.number().positive()),
  limit: requiredFor(ALGORITHMS.SLIDING_WINDOW, Joi.number().integer().min(1)),
})

const configSchema = Joi.object({
  rateLimitsPerEndpoint: Joi.array().items(ruleSchema).unique('endpoint').required(),
})

const takeRequestSchema = Joi.object({
  endpoint: Joi.string().trim().min(1).required(),
  clientId: Joi.string().trim().min(1).max(MAX_CLIENT_ID_LENGTH),
})

module.exports = { configSchema, takeRequestSchema }
