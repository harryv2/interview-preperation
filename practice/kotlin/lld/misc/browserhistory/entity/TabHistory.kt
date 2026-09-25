package lld.misc.browserhistory.entity

class TabHistory(private val maxSize: Int = 100) {
    private val pages = ArrayList<Page>()
    private var index = -1

    val current: Page?
        get() = pages.getOrNull(index)

    val canGoBack: Boolean
        get() = index > 0

    val canGoForward: Boolean
        get() = index < pages.size - 1

    // a new visit drops everything after the current page, like a real browser
    fun visit(page: Page) {
        while (pages.size > index + 1) {
            pages.removeLast()
        }
        pages += page
        index++

        if (pages.size > maxSize) {
            pages.removeFirst()
            index--
        }
    }

    fun back(steps: Int = 1): Page? {
        index = maxOf(0, index - steps)
        return current
    }

    fun forward(steps: Int = 1): Page? {
        index = minOf(pages.size - 1, index + steps)
        return current
    }
}
