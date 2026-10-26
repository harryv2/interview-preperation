# Canvas with Rectangles

Rippling LLD round. A 2D grid canvas, `draw()` rectangles, `move()` them, print the canvas.

- `Rectangle` — id, top left (x, y), width, height, fill char. Immutable, a move returns a copy
- `Canvas` — rectangles in a `LinkedHashMap`, insertion order = z order (last is on top). `move` removes and reinserts, so a moved rectangle comes to the front
- `render` — paint each rectangle in z order onto a char grid
- `topAt(x, y)` — last rectangle in z order that covers the point

Out of bounds (the follow-up they ask): `BoundsPolicy.REJECT` throws, `CLAMP` pins the rectangle to the nearest edge. A third option is to allow it and clip in `render`.

Follow-ups:
- infinite grid — no width / height, `render(viewport)` takes the window to paint. Store shapes only, never the grid
- other shapes — `Shape` interface with `contains(x, y)` and `movedBy`, render asks each cell which shape covers it
- bring to front / send to back, resize, undo (keep a stack of previous states)
