package lld.booking.eventbooking.entity

import java.time.Duration
import java.time.Instant


// holds expire on a wall clock, and a test that has to sleep for eight minutes is not a test
interface Clock {
    fun now(): Instant
}

class SystemClock : Clock {
    override fun now(): Instant {
        return Instant.now()
    }
}

class FixedClock(private var instant: Instant) : Clock {

    override fun now(): Instant {
        return instant
    }

    fun advance(by: Duration) {
        instant = instant.plus(by)
    }
}
