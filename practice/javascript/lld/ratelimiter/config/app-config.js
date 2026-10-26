const { DEFAULT_PORT, DEFAULT_MAX_LIMITERS } = require('../constants')

module.exports = {
  port: Number(process.env.PORT) || DEFAULT_PORT,
  maxLimiters: DEFAULT_MAX_LIMITERS,
}
