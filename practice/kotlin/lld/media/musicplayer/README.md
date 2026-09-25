# Music Player

- `entity/` — `Song`, `Playlist`, `PlaybackState` + `RepeatMode`, `PlayerListener`, `Player`
- `strategies/` — `PlayOrder`: `SequentialOrder`, `ShuffleOrder` (maps a position in the playlist to a song index)

`Player` holds the loaded playlist, a position, a play order and a repeat mode. `play / pause / stop / next / previous` change state and notify listeners (Observer, for the UI). `next` at the end either stops, wraps (`ALL`), or stays (`ONE`).

Follow-ups: queue "play next" (insert after position), seek within a song (add `elapsedSec`), persistence of playlists.
