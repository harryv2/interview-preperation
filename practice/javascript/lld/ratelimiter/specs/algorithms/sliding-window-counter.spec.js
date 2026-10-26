const { expect } = require('chai')
const { SlidingWindowCounter } = require('../../algorithms/sliding-window-counter')
const { FakeClock } = require('../helpers/fake-clock')

describe('SlidingWindowCounter', () => {
  let clock

  beforeEach(() => {
    clock = new FakeClock()
  })

  function createCounter(limit, windowMs) {
    return new SlidingWindowCounter({ limit, windowMs, clock })
  }

  function takeAll(counter) {
    let accepted = 0
    while (counter.tryTake().accepted) {
      accepted++
    }
    return accepted
  }

  it('allows up to the limit within a window', () => {
    const counter = createCounter(3, 10000)

    expect(counter.tryTake().remainingTokens).to.equal(2)
    expect(counter.tryTake().remainingTokens).to.equal(1)
    expect(counter.tryTake().remainingTokens).to.equal(0)
    expect(counter.tryTake().accepted).to.equal(false)
  })

  it('counts the previous window by how much of it still overlaps', () => {
    const counter = createCounter(10, 10000)
    takeAll(counter)

    clock.advance(15000)

    expect(takeAll(counter)).to.equal(5)
  })

  it('never allows much more than the limit across a window boundary', () => {
    const counter = createCounter(100, 60000)

    clock.advance(59000)
    const beforeBoundary = takeAll(counter)

    clock.advance(2000)
    const afterBoundary = takeAll(counter)

    expect(beforeBoundary).to.equal(100)
    expect(afterBoundary).to.be.at.most(4)
  })

  it('forgets everything after two idle windows', () => {
    const counter = createCounter(3, 10000)
    takeAll(counter)

    clock.advance(20000)

    expect(takeAll(counter)).to.equal(3)
  })

  it('returns when the next request will be allowed', () => {
    const counter = createCounter(2, 10000)
    takeAll(counter)

    const result = counter.tryTake()
    expect(result.retryAfterMs).to.equal(15000)

    clock.advance(14999)
    expect(counter.tryTake().accepted).to.equal(false)

    clock.advance(1)
    expect(counter.tryTake().accepted).to.equal(true)
  })

})
