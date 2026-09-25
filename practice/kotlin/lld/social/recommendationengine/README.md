# Recommendation Engine

Recommend items for a user from their interactions, item attributes and popularity.

## Layout

- `entity/` — `User`, `Item`, `Interaction` (VIEW / LIKE / PURCHASE with weights), `ScoredItem`, `ItemCatalog`, `InteractionStore`, `RecommendationService`
- `strategies/recommendation/` — `RecommendationStrategy`: `PopularityStrategy`, `ContentBasedStrategy` (tag overlap with what the user interacted with), `CollaborativeStrategy` (items from users who interacted with the same items), `HybridStrategy` (normalise each to 0..1, weighted sum)
- `strategies/filters/` — `Filter` pipeline: `AlreadyPurchasedFilter`, `OutOfStockFilter`

## Flow

1. no interactions for the user → cold-start strategy (popularity), else the main strategy
2. strategy returns candidates with score + reason
3. filters run in order
4. sort by score, dedupe, take `limit`

## Patterns

- Strategy — algorithms
- Composite — `HybridStrategy` is a strategy of strategies
- Pipeline — filters
