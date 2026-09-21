package lld.browserhistory.entity

class Tab(val id: Int) {
    val history = TabHistory()

    val current: Page?
        get() = history.current
}
