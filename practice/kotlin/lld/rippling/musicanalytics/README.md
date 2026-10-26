# Music Player Analytics

Reported Rippling screen. `addSong(title)`, `play(songId, userId)`, `printAnalytics()` sorted by unique listeners, then the last 3 songs a user played (`recentlyPlayed(userId)`).

- `Song` — id, title, set of user ids that played it. Unique listens = set size, so replays by the same user do not count
- `IndexedPriorityQueue<K, T>` — generic binary heap plus a key -> array index map. Takes a comparator and a key selector. `add`, `update`, `remove(key)`, `poll` are O(log n), `contains` / `get` / `peek` O(1), `top(k)` O(k log k) read only. Here K = song id, ordered by (unique listens desc, title)
- `MusicPlayer` — songs by id, the heap, and per user a `LinkedHashSet` of recent song ids capped at 3. Replay = remove + add, which moves it to the end in O(1)

`play` is O(log n): only a new listener changes the count, and then the song is sifted up. `topSongs(k)` reads without touching the heap: a small frontier heap of indices starts at the root, pop the best index, push its two children. k pops, frontier never above k + 1, so O(k log k).

Simpler alternative to mention: sort on every read, O(n log n), no extra structure.

Follow-ups:
- `TreeSet` ordered by (listens desc, id) does the same job: remove and reinsert on change, top k is the first k entries
- star / unstar songs per user, `topStarred(k)` — same shape as listeners
- recent limit configurable per user
