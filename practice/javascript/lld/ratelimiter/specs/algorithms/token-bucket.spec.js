const { expect } = require('chai')
const { TokenBucket } = require('../../algorithms/token-bucket')
const { FakeClock } = require('../helpers/fake-clock')

describe('TokenBucket', () => {
  let clock

  beforeEach(() => {
    clock = new FakeClock()
  })

  function createBucket(burst, sustained, windowMs = 60000) {
    return new TokenBucket({ capacity: burst, refillPerWindow: sustained, windowMs, clock })
  }

  it('starts full and allows a burst', () => {
    const bucket = createBucket(10, 6)

    for (let expectedRemaining = 9; expectedRemaining >= 0; expectedRemaining--) {
      const result = bucket.tryTake()
      expect(result.accepted).to.equal(true)
      expect(result.remainingTokens).to.equal(expectedRemaining)
    }
  })

  it('rejects with 0 remaining once the burst is used', () => {
    const bucket = createBucket(10, 6)
    for (let i = 0; i < 10; i++) {
      bucket.tryTake()
    }

    const result = bucket.tryTake()

    expect(result.accepted).to.equal(false)
    expect(result.remainingTokens).to.equal(0)
  })

  it('refills one token every 60000 / sustained ms', () => {
    const bucket = createBucket(10, 6)
    for (let i = 0; i < 10; i++) {
      bucket.tryTake()
    }

    clock.advance(9999)
    expect(bucket.tryTake().accepted).to.equal(false)

    clock.advance(1)
    expect(bucket.tryTake().accepted).to.equal(true)
    expect(bucket.tryTake().accepted).to.equal(false)
  })

  it('refills as soon as possible, 120/min gives a token every 500ms', () => {
    const bucket = createBucket(1, 120)
    bucket.tryTake()

    clock.advance(499)
    expect(bucket.tryTake().accepted).to.equal(false)

    clock.advance(1)
    expect(bucket.tryTake().accepted).to.equal(true)
  })

  it('refills over a configurable window, 10 per second gives a token every 100ms', () => {
    const bucket = createBucket(1, 10, 1000)
    bucket.tryTake()

    clock.advance(99)
    expect(bucket.tryTake().accepted).to.equal(false)

    clock.advance(1)
    expect(bucket.tryTake().accepted).to.equal(true)
  })


  it('never refills above burst', () => {
    const bucket = createBucket(10, 6)
    bucket.tryTake()

    clock.advance(60 * 60 * 1000)

    expect(bucket.tryTake().remainingTokens).to.equal(9)
  })

  it('allows burst plus what refilled meanwhile, 100 burst and 50/min gives 125 in 30s', () => {
    const bucket = createBucket(100, 50)
    let accepted = 0

    for (let i = 0; i < 100; i++) {
      if (bucket.tryTake().accepted) {
        accepted++
      }
    }

    clock.advance(30000)
    while (bucket.tryTake().accepted) {
      accepted++
    }

    expect(accepted).to.equal(125)
  })

  it('keeps partial refill progress between calls', () => {
    const bucket = createBucket(1, 6)
    bucket.tryTake()

    clock.advance(4000)
    expect(bucket.tryTake().accepted).to.equal(false)

    clock.advance(6000)
    expect(bucket.tryTake().accepted).to.equal(true)
  })

  it('returns how long to wait for the next token', () => {
    const bucket = createBucket(1, 6)
    bucket.tryTake()

    clock.advance(4000)

    expect(bucket.tryTake().retryAfterMs).to.equal(6000)
  })

  it('ignores time going backwards', () => {
    const bucket = createBucket(1, 6)
    bucket.tryTake()

    clock.advance(-5000)

    expect(bucket.tryTake().accepted).to.equal(false)
  })
})
