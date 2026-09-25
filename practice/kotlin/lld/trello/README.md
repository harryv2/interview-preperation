Trello / Task Tracker

1. Requirements

In scope
- A board holds ordered lists, a list holds ordered cards
- Move a card within a list and across lists, and the order sticks
- Assign users to a card, a card can have several
- Filter and search cards by list, assignee, label, text and due date, and combine those filters

Out of scope
- permissions and roles, activity history, comments, checklists, attachments
- real time updates to other viewers, offline editing
- search across boards, notifications, archive and undo

2. Entities

User     id, name
Card     title, description, listId, position, assignees, labels, dueAt
CardList id, name, its cards
Board    its lists, plus a cardId to card index
Filter   a predicate over a card, composable with and / or / not

Board owns the lists. A list owns its cards. A card knows which list holds it.

3. Class design

    Board
      addList(name): CardList
      addCard(listId, title, index): Card
      moveCard(cardId, toListId, toIndex): Card
      assign(cardId, userId): Card
      card(cardId): Card
      search(filter): List<Card>

    CardList
      cards(): List<Card>
      add(card, index)
      moveWithin(cardId, toIndex)
      positionAt(index, excluding): Long

    CardFilter
      matches(card): Boolean
      and / or / not

4. Key decisions

Ordering is a sparse position, not a dense index. Cards sit 100 apart, and a card dropped between two others
takes the midpoint of their positions:

    append, append     ->  100, 200
    drop between them  ->  100, 150, 200

One row is written per move and no neighbour changes, so two people reordering different parts of one list do
not fight. A dense 0,1,2,3 column would renumber every card below the drop.

The cost, stated: halving a gap of 100 runs out after about six inserts into the same slot. positionAt then
respaces the list and retries once. Demo shows it:

    six wedged into one gap    100, 101, 103, 106, 112, 125, 150, 200
    the seventh forces it      100, 150, 200, 300, 400, 500, 600, 700, 800

GAP is the only dial. Jira uses LexoRank, a base 36 string read as a fraction, which subdivides forever at the
cost of variable length keys. Better at scale, not worth deriving at a whiteboard.

Search is a scan. A board holds hundreds of cards, so filtering them is microseconds and the design question is
how filters compose, not how to index them. CardFilter is a fun interface with and / or / not, so
assignedTo(ravi) and labelled("bug") reads as the query it is. Inverted indexes on assignee, label and tokens
would matter if search went cross board, and that is where they belong, not here.

Board is the only thing that mutates structure. A move touches two lists and the card index at once, so it
cannot live on either list. Card.listId and Card.position are set by CardList so the two cannot drift.

Cards sort by position then by id. Two clients can compute the same midpoint while offline and the board still
has to agree on an order.

5. Extensibility

- ordered lists as well as ordered cards: lists get a position field and the same positionAt logic
- roles and permissions: a check at the top of each Board mutator, or a policy object if there are several rules
- activity history: Board already funnels every mutation, so each one appends an entry
- comments and checklists: fields on Card, and text search then has to decide whether to read them
- respace as a background job rather than on someone's drag, which is what a real system would do
