package lld.meetingscheduler.strategies

import lld.meetingscheduler.entity.Room

class SmallestFitRoomStrategy : RoomSelectionStrategy {
    override fun select(freeRooms: List<Room>, headcount: Int): Room? {
        return freeRooms
            .filter { it.capacity >= headcount }
            .minByOrNull { it.capacity }
    }
}
