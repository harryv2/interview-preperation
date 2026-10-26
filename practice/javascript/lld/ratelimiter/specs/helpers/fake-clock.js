class FakeClock {
  constructor(startMs = 0) {
    this.currentTime = startMs
  }

  now() {
    return this.currentTime
  }

  advance(ms) {
    this.currentTime += ms
  }
}

module.exports = { FakeClock }
