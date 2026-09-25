Stock Price Volatility Monitor

1. Requirements

In scope
- A continuous stream of trades for a symbol, arriving in order
- Track volatility over a sliding window of the last N trades
- Maximum absolute price change between consecutive trades inside that window
- High, low and range over the same window
- Many symbols at once, and the noisiest of them right now

Out of scope
- time based windows, last 5 minutes rather than last N trades. See Extensibility
- standard deviation and the other statistical definitions of volatility
- out of order or corrected trades, which a real tape has and which break a sliding window
- persistence and replay, bid ask spread, volume weighting

2. Entities

Price            exact paise, never a Double
MonotonicWindow  sliding window extreme in O(1) amortised
VolatilityMonitor one symbol's stream and the three windows over it
Snapshot         what the window says right now
MarketMonitor    a monitor per symbol, and the ranking across them

3. Class design

    MonotonicWindow(size, beats)
      add(value)
      extreme(): Long?
      forMax(size) / forMin(size)

    VolatilityMonitor(symbol, windowSize)
      record(price)
      maxTickMove(): Price?      <- the question as asked
      range(): Price?            <- high minus low, the other reading
      snapshot(): Snapshot?

    MarketMonitor(windowSize)
      record(symbol, price)
      snapshot(symbol): Snapshot?
      mostVolatile(limit): List<Snapshot>

4. Key decisions

Every question here is a sliding window extreme, and the monotonic deque is the answer to that. The insight
is that a sample can be discarded the moment a later sample beats it: it is both older and worse, so no
window that still contains the newer one will ever have it as the answer. What is left is a monotonic run of
candidates and the front of it is the extreme. Each sample is pushed once and popped once, so a push that
pops twenty is still O(1) amortised.

The alternatives are worse in the ways that matter. Rescanning the window is O(N) per trade, which on a tape
is the whole cost. A heap is O(log N) but cannot remove the trade that just left the window without lazy
deletion, so it grows without bound on a monotone tape. A running max cannot be maintained at all, because
the max leaving the window leaves nothing behind to fall back to.

Candidates age out by arrival number, not by value. Evicting by comparing values is the bug that a flat tape
finds immediately: with 50, 50, 50 in the window, the copy that is leaving looks exactly like the copies that
are staying, and the window loses a high it still holds. Every sample carries the count of trades seen before
it, and eviction is a comparison against that.

The move window is one shorter than the price window. N trades produce N - 1 consecutive changes, and sizing
both windows the same is the off by one that keeps reporting a jump computed from a trade that has already
left. It is one line and it is the thing to check first if the numbers look wrong.

Prices are Long paise. A monitor whose entire job is comparing small differences is the last place to accept
a representation where the difference of two exactly representable inputs is not exact, and the comparisons
inside the deque would inherit the error.

Both readings of volatility are kept, because they answer different questions and the demo shows a tape where
they disagree completely. A steady climb from 100 to 108 and a spike to 108 and back have the same range of
8, and maximum tick move is the only one of the two that says which tape you are looking at.

Sharding is by symbol and that is the whole concurrency design. Two symbols share no state, so two threads on
different symbols never meet, and one symbol is a single ordered stream by definition. The lock inside a
monitor exists only so a reader asking for a snapshot cannot catch a record half done, with the price windows
updated and the move window not.

5. Extensibility

- time based windows: the deque already carries a sequence number per sample, so making it a timestamp and
  evicting on now - duration is a change to one comparison. The move window's size rule disappears with it
- more statistics: standard deviation needs running sums rather than extremes, which is a different structure
  next to these rather than a change to them. Mean and sum are O(1) with a ring buffer and a running total
- alerting: mostVolatile is already the read a desk wants. A threshold per symbol and a callback on record
  turns this into the alerting half without touching the windows
- out of order trades: this design assumes arrival order is trade order. Corrections and late prints need a
  window keyed on trade time with a grace period, and at that point the deque stops being enough
- very large N or very many symbols: the deque is O(N) worst case in memory per window, three per symbol. A
  tape wide enough for that wants the windows bucketed and merged rather than exact
