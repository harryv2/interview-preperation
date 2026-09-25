package lld.booking.meetingscheduler.strategies

import lld.booking.meetingscheduler.entity.Room

class SmallestFitRoomStrategy : RoomSelectionStrategy {
    override fun select(freeRooms: List<Room>, headcount: Int): Room? {
        return freeRooms
            .filter { it.capacity >= headcount }
            .minByOrNull { it.capacity }
    }
}
