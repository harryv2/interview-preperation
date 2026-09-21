package lld.recommendationengine.strategies.filters

import lld.recommendationengine.entity.ScoredItem
import lld.recommendationengine.entity.User

fun interface Filter {
    fun apply(user: User, items: List<ScoredItem>): List<ScoredItem>
}
