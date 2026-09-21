package lld.browserhistory.entity

data class Page(
    val url: String,
    val title: String,
    val visitedAt: Long = System.currentTimeMillis(),
)
