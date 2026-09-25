Kotlin LLD problems, grouped by domain.

Each folder is one self contained problem: entities, whatever strategy or service layers it needs, a Demo.kt
with a main, and a README.md with the requirements, the class design, the decisions worth defending, and
where it would extend. Nothing imports across problems.

booking/       reserve a finite resource for a window of time
  carrental, deliveryslot, eventbooking, hotel, library, meetingscheduler,
  parkingbooking, restaurantreservation, slotbooking

commerce/      ordering, fulfilment and stock
  amazonlocker, amazonlockersimple, amazonlockersimpledelivery,
  inventorymanagement, restaurantordering, zomatooms

fintech/       money, ledgers and market data
  cryptowallet, orderbook, splitwise, volatilitymonitor

games/         turn taking, boards and ranking
  chess, leaderboard, snakeladder

media/         pipelines over audio and playlists
  audiopipeline, musicplayer

messaging/     fan out, delivery and escalation
  notificationengine, oncallalerting, pubsub, pubsubcoroutines

social/        content, feeds and growth
  quora, recommendationengine, referral

misc/          no domain peer yet
  browserhistory, elevator, ruleengine, trello

The package of a file always matches its path, lld.<group>.<problem>[.<layer>], so a problem can be moved
between groups by renaming its package and nothing else.
