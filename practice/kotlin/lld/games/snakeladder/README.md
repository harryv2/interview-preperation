Snake and Ladder

1. Requirements

In scope
- A board of numbered cells, every player starts on cell 1 and races to the last one
- Snakes send you back, ladders send you forward, one jump per landing
- Roll a six and you roll again
- You need the exact number to finish, an overshoot does not move you
- Reject a board that is built wrong, rather than defending against it on every roll

Out of scope
- more than one token per player, blocking or capturing on a shared cell
- networked or persisted games, turn timeouts, spectators
- board generation tuned to a target game length

2. Entities

Jump      from, to          a snake if to < from, a ladder if to > from
Board     size, jumps       owns the cells and validates itself
Player    id, name
Move      what one roll did, so the log reads without reconstructing it
Game      turn order, positions, the winner
Dice      the one interface

3. Class design

    Board(size, jumps)
      jumpAt(cell): Jump?

    Game(board, dice, players)
      playTurn(): Move
      play(maxTurns): Player?
      positionOf(player): Int

    Dice
      roll(): Int            SingleDie, ScriptedDice

4. Key decisions

A snake and a ladder are one class. Jump(from, to) and isLadder is derived. Two classes would mean two maps to
search on every landing, two validation paths, and every rule written twice.

Validation happens in the Board constructor. Two jumps from one cell, a jump starting on the winning cell or
on cell 1, a jump off the board, a jump to itself. Once the board exists it cannot be wrong, so playTurn has
no defensive checks in it.

Dice is the only interface. It is the one thing that genuinely varies, and ScriptedDice is why: a game whose
outcome depends on Random.Default can be watched but not demonstrated or tested. Everything else is a fixed
rule and got written as a fixed rule.

Roll again on a six is one line, not a strategy. The turn queue only rotates when the turn ends, so the rule
is "don't rotate" and nothing else has to know about it.

5. Extensibility

- overshoot wins instead of exact finish: one branch in playTurn, or a MoveRule interface if the interviewer
  wants both at once
- three sixes forfeits the turn: a consecutive counter on Game, reset in endTurn
- chained jumps, a ladder that drops you onto a snake: loop in playTurn instead of one lookup, and the Board
  constructor then has to reject cycles too
- a move log or live updates: Game already returns a Move per turn, so a listener list is the only addition
