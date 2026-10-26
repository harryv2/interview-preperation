const { expect } = require('chai')
const { parseRateLimitRules } = require('../../config/parse-rate-limit-rules')
const rateLimitConfig = require('../../config.json')

describe('parseRateLimitRules', () => {
  function configWith(rules) {
    return { rateLimitsPerEndpoint: rules }
  }

  function rulesFrom(rules) {
    return parseRateLimitRules(configWith(rules))
  }

  it('reads the rules from config.json with defaults applied', () => {
    const rules = parseRateLimitRules(rateLimitConfig)

    expect(rules).to.deep.include({
      endpoint: 'GET /user/:id',
      algorithm: 'token-bucket',
      windowMs: 60000,
      burst: 10,
      sustained: 6,
    })
  })

  it('defaults to token bucket over one minute', () => {
    const [rule] = rulesFrom([{ endpoint: 'GET /a', burst: 5, sustained: 1 }])

    expect(rule).to.deep.equal({ endpoint: 'GET /a', algorithm: 'token-bucket', windowMs: 60000, burst: 5, sustained: 1 })
  })

  it('accepts a custom window', () => {
    const [rule] = rulesFrom([{ endpoint: 'GET /a', burst: 5, sustained: 1, windowMs: 1000 }])

    expect(rule.windowMs).to.equal(1000)
  })

  it('accepts a sliding window rule', () => {
    const [rule] = rulesFrom([{ endpoint: 'POST /login', algorithm: 'sliding-window', limit: 5, windowMs: 900000 }])

    expect(rule).to.deep.equal({ endpoint: 'POST /login', algorithm: 'sliding-window', windowMs: 900000, limit: 5 })
  })

  it('rejects a missing rules array', () => {
    expect(() => parseRateLimitRules({})).to.throw('"rateLimitsPerEndpoint" is required')
  })

  it('rejects an unknown algorithm', () => {
    expect(() => rulesFrom([{ endpoint: 'GET /a', algorithm: 'leaky-bucket', limit: 1 }])).to.throw('algorithm')
  })

  it('rejects a non-positive window', () => {
    expect(() => rulesFrom([{ endpoint: 'GET /a', burst: 1, sustained: 1, windowMs: 0 }])).to.throw('windowMs')
  })

  it('rejects an empty endpoint', () => {
    expect(() => rulesFrom([{ endpoint: '', burst: 1, sustained: 1 }])).to.throw('endpoint')
  })

  it('rejects burst below 1', () => {
    expect(() => rulesFrom([{ endpoint: 'GET /a', burst: 0, sustained: 1 }])).to.throw('burst')
  })

  it('rejects a fractional burst', () => {
    expect(() => rulesFrom([{ endpoint: 'GET /a', burst: 1.5, sustained: 1 }])).to.throw('burst')
  })

  it('rejects non-positive sustained', () => {
    expect(() => rulesFrom([{ endpoint: 'GET /a', burst: 1, sustained: 0 }])).to.throw('sustained')
  })

  it('rejects options that belong to another algorithm', () => {
    expect(() => rulesFrom([{ endpoint: 'GET /a', algorithm: 'sliding-window', limit: 1, burst: 5 }])).to.throw('rateLimitsPerEndpoint[0].burst" is not allowed')
  })

  it('rejects a sliding window rule without a limit', () => {
    expect(() => rulesFrom([{ endpoint: 'GET /a', algorithm: 'sliding-window' }])).to.throw('rateLimitsPerEndpoint[0].limit" is required')
  })

  it('rejects duplicate endpoints', () => {
    const rules = [
      { endpoint: 'GET /a', burst: 1, sustained: 1 },
      { endpoint: 'GET /a', burst: 2, sustained: 2 },
    ]

    expect(() => rulesFrom(rules)).to.throw('duplicate')
  })

  it('reports every problem in a rule at once', () => {
    expect(() => rulesFrom([{ endpoint: 'GET /a', burst: 0, sustained: 0 }])).to.throw(/burst.*sustained/)
  })

  it('says which rule is invalid', () => {
    const rules = [
      { endpoint: 'GET /a', burst: 1, sustained: 1 },
      { endpoint: 'GET /b', burst: 0, sustained: 1 },
    ]

    expect(() => rulesFrom(rules)).to.throw('rateLimitsPerEndpoint[1].burst')
  })
})
