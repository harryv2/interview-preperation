On-Call Alerting System

1. Requirements

In scope
- Many projects, each with its own escalation matrix
- A level is a set of responders, the channels they are woken on, and how long they have to answer
- An incident pages level 0, and climbs a level every time nobody acknowledges in the window
- IVR, SMS and Email as channels, and a channel being down must not silence the others
- Acknowledge stops the climb, resolve closes the incident

Out of scope
- on call rotations and calendars. The matrix here is a fixed list of people, see Extensibility
- deduplication and alert grouping, maintenance windows, incident severity driving a different matrix
- real carriers, delivery receipts, and the retry behaviour of a single channel
- persistence, so a restart forgets every armed timer

2. Entities

Responder        who gets woken, and the address to use per channel
ChannelType      EMAIL, SMS, IVR
EscalationLevel  responders, channels, waitFor
EscalationPolicy the levels in order, level 0 first
Project          a service with a policy
Incident         status, the level it has reached, who acknowledged, and a timeline
AlertChannel     how a page actually goes out
AlertingService  raises incidents, fans out a page, arms and cancels the escalation timer

3. Class design

    EscalationPolicy(levels)
      levelAt(index): EscalationLevel
      knows(responder): Boolean

    Incident(id, project, title, severity)
      status, level, acknowledgedBy
      escalate(from): Boolean        <- from is the level the timer was armed for
      acknowledge(responder): Boolean
      resolve(responder): Boolean
      timeline(): List<String>

    AlertingService(channels)
      register(project)
      raise(projectId, title, severity): Incident
      acknowledge(incidentId, responder): Boolean
      resolve(incidentId, responder): Boolean

    AlertChannel
      page(responder, incident)      EmailChannel, SmsChannel, IvrChannel

4. Key decisions

Escalation is a timer, not a poll. Raising an incident pages level 0 and arms one scheduled task for that
level's window. Nothing sweeps a list of open incidents looking for stale ones, because the wait is already
known at the moment the page goes out, and a sweep would have to run far more often than it finds work.

The timer carries the level it was armed for, and that is the whole concurrency story. Acknowledging cancels
the pending task, but cancel() cannot stop a task that is already inside its run method, so a page and an
acknowledgement that land at the same instant are a genuine race. escalate(from) takes the incident lock and
refuses unless the status is still TRIGGERED and the level is still the one the timer was armed for:

    if (status != TRIGGERED) return false
    if (level != from) return false

Cancelling is the optimisation and the guard is the correctness. Dropping the cancel would only waste a
wakeup, dropping the guard would wake someone at 3am for an incident that was handled a millisecond earlier.

Acknowledging twice is not an error. The same page goes out on SMS and IVR, and a responder who answers both
is behaving normally, so the second call returns false meaning already handled rather than throwing. The same
is true of a responder who answers just as the page escalates past them.

The fan out is best effort per channel. A level with two channels and three responders is six independent
sends, and one carrier timing out must not stop the other five, so each send is wrapped and a failure is
recorded on the timeline rather than propagated. Silence is the one outcome an alerting system may not
produce. FlakyChannel exists to show this in the demo.

The last level does not wrap or repeat. Running out of matrix is recorded on the incident as escalation
exhausted and the incident stays TRIGGERED, because the honest thing at that point is a visible unanswered
incident rather than a timer that keeps paging the same person forever.

The incident owns its own lock rather than the service holding one. Incidents on different projects, and on
the same project, have nothing to contend over, and the escalation timer thread touching one incident should
never block an acknowledgement for another.

5. Extensibility

- on call rotations: EscalationLevel resolves responders through a Schedule rather than holding a list, so
  who is on call is a function of the time the page goes out. Nothing else in the flow moves
- severity driven matrices: Project holds a policy per severity, and raise picks one. A SEV3 that starts at
  email and never calls anyone is the usual reason to want this
- repeat notifications: a level that re pages the same responders every interval before moving on, which is
  an extra timer at the same level rather than a new concept
- deduplication: a fingerprint on the incident and a lookup before raise, so a flapping check joins the open
  incident instead of starting a new page
- per channel retry: today a failed send is recorded and dropped. A RetryPolicy on AlertChannel is the seam,
  and it stays inside the channel so escalation timing does not have to know about it
- persistence and more than one server: the armed timer is in memory, so a restart loses every pending
  escalation. Durable timers, or a next_escalation_at column and a sweeper that claims rows, is the fix, and
  at that point the escalate() guard becomes a conditional update rather than a check under a lock
