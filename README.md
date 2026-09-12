# interview-problems

Personal coding interview practice in Kotlin and Go.

## What's here

- **Data structures from scratch** — linked lists, stack, queue, BST, AVL tree (following the mycodeschool series)
- **Problem solutions** — LeetCode-style DSA problems
- **Low-level design** — object-oriented modelling exercises (elevator, expense splitting, Q&A service)
- **Interview questions** — problems from actual interviews

## Running

No build tool or dependencies. Every file has its own `main()` that acts as a demo / smoke test.

- **Kotlin** — open the repo root in IntelliJ IDEA and run any `main()` from the gutter.
- **Go** — `go run <file>`

## Conventions

- Public APIs follow the Kotlin stdlib where it makes sense: `size` as a property, `isEmpty()`, `operator fun get` / `contains` so `list[i]` and `x in list` work.
- `require` for bad arguments, `check` for bad state.
- Generic containers store `Array<Any?>` and cast on read, like `kotlin.collections.ArrayDeque`.
