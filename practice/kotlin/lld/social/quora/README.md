Quora / Q and A System

- User asks a question, tags it with up to five topics
- Others answer, and comment on either a question or an answer
- Comments are two levels only, a reply to a reply flattens onto the same thread
- Any content can be upvoted or downvoted, one vote per user per item
- Voting the same way twice toggles the vote off, voting the other way is a swing of two
- Nobody votes on their own content
- Answers are ordered by a swappable ranking
- A profile shows counts and reputation without recounting votes

Entities

User, UserStats
Content (Question, Answer, Comment)
Topic
Vote

Vote outcome

none -> ADDED
same button again -> REMOVED
other button -> SWITCHED

Decisions worth defending

- Content is a base class and not an interface, the vote counting implementation is shared and not just the contract
- Tallies are stored on the content and stats on the user, recounting every vote on every profile load does not scale
- Two copies of the truth need a reconciler, VoteService.reconcile recounts from the vote records
- The vote map is keyed voterId:type:targetId, which is what makes the toggle O(1) instead of a scan
- Question owns answerIds and topicIds, nothing outside can append and skip the closed or deleted check
- Comment builds its own reply, so the flattening rule lives with the thing it constrains
- Topic indexes its questions both ways, a topic page would otherwise scan every question in the system

Swappable
- answer ranking (score, newest, up ratio)

Not modelled
- follows and feeds, notifications, search ranking, spaces, moderation, edit history, accepted answers
