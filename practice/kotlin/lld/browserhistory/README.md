# Browser History

- `Page` — url, title, visited time
- `TabHistory` — one list plus an index. `visit` cuts everything after the index (forward pages are lost), `back` / `forward` move the index, capped at `maxSize`
- `Tab` — owns a `TabHistory`
- `Browser` — tabs (open / switch / close), delegates `visit` / `back` / `forward` to the active tab, and keeps a flat global history for `recent` and `search`

Two stacks (back / forward) also work; the list + index version is simpler and supports `back(n)` in O(1).

Follow-ups: persistence, incognito tabs (no global history), dedupe of the same url visited twice in a row.
