# In-Memory Cron Scheduler

Register jobs that run every N ms / minutes / hours. Follow-up: a run that takes longer than its interval must not pile up or lock the system.

- `Job` — id, interval, overlap policy, action, `nextRunAt`, counters
- `queue` — min heap by `nextRunAt`
- ticker thread — waits on a condition until the head is due (`await(timeout)`), so it sleeps exactly as long as needed and wakes early when a job is added or cancelled
- worker pool of fixed size — the ticker only hands work off and never runs a job itself, so one slow job can't delay the others

Fixed rate with catch-up skipped: next = previous + interval, bumped forward until it's in the future. A long GC pause gives one late run, not a burst of missed ones.

Overlap (the follow-up):
| policy | the next tick fires while the last run is still going |
|---|---|
| `SKIP` | drop the tick, count it |
| `QUEUE_ONE` | remember one pending run, start it right after the current one ends. More ticks while pending are dropped |
| `ALLOW_CONCURRENT` | start another run on another worker |

The running / pending flags change under `synchronized(job)`, so "finish run" and "tick arrives" can't race and lose a pending run. Lock order is always scheduler lock -> job lock (the worker only takes the job lock), so no deadlock.

A job that throws is counted and keeps its schedule.

Follow-ups:
- real cron expressions (`*/5 * * * *`) — a `Schedule` interface with `nextAfter(time)`; interval and cron are two implementations
- distributed — one leader picks due jobs (or a DB row lock / lease per job run) so two nodes don't run the same tick
- persistence — store `nextRunAt` so a restart doesn't skip or double run
- per job timeout — `future.get(timeout)` and interrupt
