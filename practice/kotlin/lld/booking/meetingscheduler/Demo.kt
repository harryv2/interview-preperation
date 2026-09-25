package lld.booking.meetingscheduler

import lld.booking.meetingscheduler.entity.Room
import lld.booking.meetingscheduler.entity.Scheduler
import lld.booking.meetingscheduler.entity.TimeSlot
import lld.booking.meetingscheduler.entity.User
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

fun main() {
    val scheduler = Scheduler(
        listOf(
            Room("r1", "Huddle", capacity = 3),
            Room("r2", "Board room", capacity = 10),
        ),
    )
    val alice = User("u1", "Alice")
    val bob = User("u2", "Bob")
    val carol = User("u3", "Carol")

    val day = Instant.parse("2026-09-21T09:00:00Z")
    fun at(hour: Int, minute: Int = 0): Instant {
        return day + hour.hours + minute.minutes
    }

    println("-- schedule")
    val standup = scheduler.schedule("Standup", alice, setOf(bob), TimeSlot(at(0), at(0, 30)))
    println("  ${standup.title} in ${standup.room.name} ${standup.slot.start}..${standup.slot.end}")
    val review = scheduler.schedule("Design review", alice, setOf(bob, carol), TimeSlot(at(1), at(2)))
    println("  ${review.title} in ${review.room.name} ${review.slot.start}..${review.slot.end}")

    println("-- conflict: Bob is in the review")
    runCatching { scheduler.schedule("1:1", carol, setOf(bob), TimeSlot(at(1, 30), at(2))) }
        .onFailure { println("  ${it.message}") }

    println("-- free 30 min slots for Alice, Bob, Carol between 09:00 and 12:00")
    val free = scheduler.freeSlots(setOf(alice, bob, carol), 30.minutes, TimeSlot(at(0), at(3)), step = 30.minutes)
    free.forEach { println("  ${it.start}..${it.end}") }

    println("-- what is going on in each room at 10:15")
    for (roomId in listOf("r1", "r2")) {
        println("  $roomId: ${scheduler.currentMeeting(roomId, at(1, 15))?.title ?: "free"}")
    }

    println("-- both rooms taken 12:00-13:00, a third meeting has nowhere to go")
    val dave = User("u4", "Dave")
    val erin = User("u5", "Erin")
    scheduler.schedule("Hiring sync", alice, setOf(bob), TimeSlot(at(3), at(4)))
    scheduler.schedule("All hands", carol, setOf(dave), TimeSlot(at(3), at(4)))
    runCatching { scheduler.schedule("Retro", erin, emptySet(), TimeSlot(at(3), at(4))) }
        .onFailure { println("  ${it.message}") }
    println("  next 1h slot for Erin with a room: ${scheduler.freeSlots(setOf(erin), 1.hours, TimeSlot(at(3), at(6)), 30.minutes).firstOrNull()?.start}")

    println("-- cancel the review, slot opens up")
    scheduler.cancel(review.id)
    val retry = scheduler.schedule("1:1", carol, setOf(bob), TimeSlot(at(1, 30), at(2)))
    println("  ${retry.title} in ${retry.room.name}")

    println("-- Alice's calendar")
    scheduler.calendarOf(alice).all().forEach { println("  ${it.slot.start} ${it.title}") }
}
