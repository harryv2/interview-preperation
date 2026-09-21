package lld.meetingscheduler.entity

import lld.meetingscheduler.strategies.RoomSelectionStrategy
import lld.meetingscheduler.strategies.SmallestFitRoomStrategy
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid

class Scheduler(
    rooms: List<Room>,
    private val roomSelection: RoomSelectionStrategy = SmallestFitRoomStrategy(),
) {
    private val lock = ReentrantLock()
    private val rooms = rooms.associateBy { it.id }
    private val roomCalendars = rooms.associate { it.id to Calendar() }
    private val userCalendars = HashMap<String, Calendar>()
    private val meetings = HashMap<Uuid, Meeting>()

    fun schedule(title: String, organizer: User, attendees: Set<User>, slot: TimeSlot): Meeting {
        val everyone = attendees + organizer

        lock.withLock {
            val busy = everyone.filter { !calendarOf(it).isFree(slot) }
            require(busy.isEmpty()) { "Not free: ${busy.map { it.name }}" }

            val room = roomSelection.select(freeRooms(slot), everyone.size)
            requireNotNull(room) { "No free room for ${everyone.size} people at ${slot.start}" }

            val meeting = Meeting(Uuid.random(), title, organizer, everyone, room, slot)
            meetings[meeting.id] = meeting
            roomCalendars.getValue(room.id).add(meeting)
            everyone.forEach { calendarOf(it).add(meeting) }
            return meeting
        }
    }

    fun cancel(meetingId: Uuid) {
        lock.withLock {
            val meeting = requireNotNull(meetings.remove(meetingId)) { "Meeting $meetingId not found" }
            roomCalendars.getValue(meeting.room.id).remove(meetingId)
            meeting.attendees.forEach { calendarOf(it).remove(meetingId) }
        }
    }

    // slots where every person is free and some room with enough capacity is free
    fun freeSlots(people: Set<User>, duration: Duration, within: TimeSlot, step: Duration): List<TimeSlot> {
        val found = ArrayList<TimeSlot>()
        var start = within.start

        lock.withLock {
            while (start + duration <= within.end) {
                val candidate = TimeSlot(start, start + duration)
                val peopleFree = people.all { calendarOf(it).isFree(candidate) }
                val roomFree = roomSelection.select(freeRooms(candidate), people.size) != null
                if (peopleFree && roomFree) {
                    found += candidate
                }
                start += step
            }
        }
        return found
    }

    fun currentMeeting(roomId: String, at: Instant = Clock.System.now()): Meeting? {
        val calendar = requireNotNull(roomCalendars[roomId]) { "Room $roomId not found" }
        lock.withLock {
            return calendar.meetingAt(at)
        }
    }

    fun calendarOf(user: User): Calendar {
        return userCalendars.getOrPut(user.id) { Calendar() }
    }

    private fun freeRooms(slot: TimeSlot): List<Room> {
        return rooms.values.filter { roomCalendars.getValue(it.id).isFree(slot) }
    }
}
