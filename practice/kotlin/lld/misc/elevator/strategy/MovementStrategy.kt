package lld.misc.elevator.strategy

import lld.misc.elevator.entity.Direction
import java.util.NavigableSet

// =====================================================================
//  MOVEMENT STRATEGY  --  in what ORDER does ONE car serve its stops?
//
//  CONTRACT: the returned floor must lie in the current direction, or
//  be null. Return a floor behind the car and step() will drive the
//  wrong way, because step() moves according to `direction`.
//
//  Note this is a different question from SchedulingStrategy, which
//  lives in the scheduler package. This one sees one car's own stops.
//  That one has to compare cars.
//
//  In a 45-minute round, leaving this as a private method on Elevator
//  is fine. Extract it if asked about express or freight cars.
// =====================================================================

fun interface MovementStrategy {
    fun nextStop(currentFloor: Int, direction: Direction, stops: NavigableSet<Int>): Int?
}
