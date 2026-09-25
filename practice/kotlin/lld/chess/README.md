Chess Board Game

- Two players alternate moves on an 8x8 board
- Each piece type has its own movement rule
- A move is legal only if it is a pseudo legal move for that piece AND it does not leave
  the mover's own king in check
- Game ends in checkmate (in check, no legal move) or stalemate (not in check, no legal move)
- A pawn reaching the far rank promotes

Entities

Color
Square
Piece (King, Queen, Rook, Bishop, Knight, Pawn)
Board
Move
Game

Design

Piece is the polymorphic bit: every subclass answers "where could I go from here, ignoring
check". Board answers "is this square attacked". Game layers the check rule on top and owns
turn order and end conditions. That split is what keeps legality in one place instead of
spread across six movement rules.

Not modelled
- castling, en passant, fifty move and threefold repetition draws, clocks, notation parsing
  beyond simple square names
