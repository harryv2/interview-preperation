package lld.social.recommendationengine.strategies.filters

import lld.social.recommendationengine.entity.ScoredItem
import lld.social.recommendationengine.entity.User

fun interface Filter {
    fun apply(user: User, items: List<ScoredItem>): List<ScoredItem>
}
