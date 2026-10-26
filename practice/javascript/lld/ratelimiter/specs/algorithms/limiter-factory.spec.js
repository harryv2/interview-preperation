const { expect } = require('chai')
const { LimiterFactory } = require('../../algorithms/limiter-factory')
const { TokenBucket } = require('../../algorithms/token-bucket')
const { SlidingWindowCounter } = require('../../algorithms/sliding-window-counter')
const { FakeClock } = require('../helpers/fake-clock')
const { buildRules } = require('../helpers/build-rules')

describe('LimiterFactory', () => {
  const clock = new FakeClock()
  const limiterFactory = new LimiterFactory(clock)

  it('creates a token bucket from a token bucket rule', () => {
    const [rule] = buildRules([{ endpoint: 'GET /a', burst: 3, sustained: 6 }])

    const limiter = limiterFactory.create(rule)

    expect(limiter).to.be.instanceOf(TokenBucket)
    expect(limiter.capacity).to.equal(3)
    expect(limiter.clock).to.equal(clock)
  })

  it('creates a sliding window counter from a sliding window rule', () => {
    const [rule] = buildRules([{ endpoint: 'POST /login', algorithm: 'sliding-window', limit: 5, windowMs: 1000 }])

    const limiter = limiterFactory.create(rule)

    expect(limiter).to.be.instanceOf(SlidingWindowCounter)
    expect(limiter.limit).to.equal(5)
    expect(limiter.windowMs).to.equal(1000)
  })

  it('throws for an unknown algorithm', () => {
    expect(() => limiterFactory.create({ algorithm: 'leaky-bucket' })).to.throw('Unknown algorithm')
  })
})
