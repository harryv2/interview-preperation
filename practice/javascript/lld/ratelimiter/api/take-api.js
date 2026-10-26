const { takeRequestSchema } = require('../utils/joi-schemas')

class TakeApi {
  constructor(rateLimiter) {
    this.rateLimiter = rateLimiter
  }

  async handleRequest(req, res) {
    const { value, error } = takeRequestSchema.validate(req.body, { abortEarly: false })

    if (error) {
      return res.status(400).json({ error: error.message })
    }

    const { endpoint, clientId } = value

    if (!this.rateLimiter.hasEndpoint(endpoint)) {
      return res.status(404).json({ error: `No rate limit configured for "${endpoint}"` })
    }

    return res.status(200).json(this.rateLimiter.take(endpoint, clientId))
  }
}

module.exports = { TakeApi }
