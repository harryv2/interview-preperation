package lld.misc.browserhistory.entity

class Browser {
    private val tabs = LinkedHashMap<Int, Tab>()
    private val globalHistory = ArrayList<Page>()
    private var nextTabId = 1

    var activeTab: Tab = newTab()
        private set

    fun newTab(): Tab {
        val tab = Tab(nextTabId++)
        tabs[tab.id] = tab
        activeTab = tab
        return tab
    }

    fun switchTo(tabId: Int) {
        activeTab = requireNotNull(tabs[tabId]) { "No tab $tabId" }
    }

    fun closeTab(tabId: Int) {
        require(tabs.size > 1) { "Cannot close the last tab" }
        tabs.remove(tabId)
        if (activeTab.id == tabId) {
            activeTab = tabs.values.last()
        }
    }

    fun visit(url: String, title: String): Page {
        val page = Page(url, title)
        activeTab.history.visit(page)
        globalHistory += page
        return page
    }

    fun back(steps: Int = 1): Page? {
        return activeTab.history.back(steps)
    }

    fun forward(steps: Int = 1): Page? {
        return activeTab.history.forward(steps)
    }

    fun search(text: String): List<Page> {
        return globalHistory.filter { it.url.contains(text, ignoreCase = true) || it.title.contains(text, ignoreCase = true) }
    }

    fun recent(limit: Int): List<Page> {
        return globalHistory.takeLast(limit).reversed()
    }

    fun clearHistory() {
        globalHistory.clear()
    }
}
