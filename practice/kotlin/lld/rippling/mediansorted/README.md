# Median of Sorted Arrays / N Streams

Two sorted salary arrays, find the median. Follow-up: N sorted arrays or streams, time and memory.

| method | idea | time | extra memory |
|---|---|---|---|
| `ofTwo` | binary search the cut in the smaller array so left halves hold `(total+1)/2` values and `aLeft <= bRight`, `bLeft <= aRight` | O(log min(m, n)) | O(1) |
| `ofMany` | binary search on the value: smallest `v` with `count(<= v) >= k`, count is an upper bound search per array | O(N · log m · log range) | O(1) |
| `ofStreams` | k-way merge with a min heap of one head per stream, stop at the middle | O(total/2 · log N) | O(N) |
| `RunningMedian` | max heap for the low half, min heap for the high half, sizes differ by at most 1 | O(log n) add, O(1) median | O(n) |

Which to pick:
- random access arrays — `ofMany`, no merge at all
- forward-only streams with a known total — `ofStreams`, only N values in memory
- unknown total, values keep arriving — `RunningMedian`. If memory is the limit and salaries are bounded integers, count per bucket (a histogram) and walk it to the middle

Sum with `Long` before dividing so two big ints don't overflow.
