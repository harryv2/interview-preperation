const { expect } = require('chai')
const { RateLimiter } = require('../../services/rate-limiter')
const { LimiterFactory } = require('../../algorithms/limiter-factory')
const { FakeClock } = require('../helpers/fake-clock')
const { buildRules } = require('../helpers/build-rules')

describe('RateLimiter', () => {
  const rules = buildRules([
    { endpoint: 'GET /user/:id', burst: 2, sustained: 6 },
    { endpoint: 'PATCH /user/:id', burst: 1, sustained: 5 },
    { endpoint: 'POST /login', algorithm: 'sliding-window', limit: 2, windowMs: 10000 },
  ])

  let clock
  let rateLimiter

  beforeEach(() => {
    clock = new FakeClock()
    rateLimiter = new RateLimiter(rules, new LimiterFactory(clock))
  })

  function drain(endpoint, clientId) {
    while (rateLimiter.take(endpoint, clientId).accepted) {
      continue
    }
  }

  it('knows which endpoints are configured', () => {
    expect(rateLimiter.hasEndpoint('GET /user/:id')).to.equal(true)
    expect(rateLimiter.hasEndpoint('GET /unknown')).to.equal(false)
  })

  it('keeps a separate bucket per endpoint', () => {
    drain('PATCH /user/:id')

    expect(rateLimiter.take('GET /user/:id').accepted).to.equal(true)
  })

  it('keeps bucket state across calls', () => {
    drain('GET /user/:id')
    expect(rateLimiter.take('GET /user/:id').accepted).to.equal(false)

    clock.advance(10000)
    expect(rateLimiter.take('GET /user/:id').accepted).to.equal(true)
  })

  it('keeps a separate bucket per client', () => {
    drain('GET /user/:id', 'client-a')

    expect(rateLimiter.take('GET /user/:id', 'client-a').accepted).to.equal(false)
    expect(rateLimiter.take('GET /user/:id', 'client-b').accepted).to.equal(true)
  })

  it('uses one shared bucket when no client is given', () => {
    drain('GET /user/:id')

    expect(rateLimiter.take('GET /user/:id').accepted).to.equal(false)
    expect(rateLimiter.take('GET /user/:id', 'client-a').accepted).to.equal(true)
  })

  it('does not mix up endpoint and client in the key', () => {
    const keyRules = buildRules([
      { endpoint: 'GET a', burst: 1, sustained: 1 },
      { endpoint: 'GET a|b', burst: 1, sustained: 1 },
    ])
    const limiter = new RateLimiter(keyRules, new LimiterFactory(clock))

    limiter.take('GET a|b', 'c')

    expect(limiter.take('GET a', 'b|c').accepted).to.equal(true)
  })

  it('evicts the least recently used bucket when full', () => {
    const limiter = new RateLimiter(rules, new LimiterFactory(clock), { maxLimiters: 2 })

    limiter.take('PATCH /user/:id', 'client-a')
    expect(limiter.take('PATCH /user/:id', 'client-a').accepted).to.equal(false)

    limiter.take('PATCH /user/:id', 'client-b')
    limiter.take('PATCH /user/:id', 'client-c')

    expect(limiter.take('PATCH /user/:id', 'client-a').accepted).to.equal(true)
  })

  it('uses the algorithm configured for each endpoint', () => {
    drain('POST /login')

    clock.advance(5000)
    expect(rateLimiter.take('POST /login').accepted).to.equal(false)

    clock.advance(10000)
    expect(rateLimiter.take('POST /login').accepted).to.equal(true)
  })

  it('creates one limiter per endpoint and client and reuses it', () => {
    const createdFor = []
    const stubFactory = {
      create: (rule) => {
        createdFor.push(rule.endpoint)
        return { tryTake: () => ({ accepted: true, remainingTokens: 0, retryAfterMs: 0 }) }
      },
    }
    const limiter = new RateLimiter(rules, stubFactory)

    limiter.take('GET /user/:id', 'client-a')
    limiter.take('GET /user/:id', 'client-a')
    limiter.take('GET /user/:id', 'client-b')

    expect(createdFor).to.deep.equal(['GET /user/:id', 'GET /user/:id'])
  })

  it('throws for an unknown endpoint', () => {
    expect(() => rateLimiter.take('GET /unknown')).to.throw('No rate limit configured')
  })
})
