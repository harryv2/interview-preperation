package lld.misc.browserhistory

import lld.misc.browserhistory.entity.Browser

fun main() {
    val browser = Browser()

    println("-- back / forward in one tab")
    browser.visit("google.com", "Google")
    browser.visit("github.com", "GitHub")
    browser.visit("kotlinlang.org", "Kotlin")
    println("  current: ${browser.activeTab.current?.url}")
    println("  back:    ${browser.back()?.url}")
    println("  back:    ${browser.back()?.url}")
    println("  forward: ${browser.forward()?.url}")

    println("-- a new visit drops the forward pages")
    browser.visit("stackoverflow.com", "Stack Overflow")
    println("  forward possible: ${browser.activeTab.history.canGoForward}")
    println("  back:    ${browser.back()?.url}")

    println("-- tabs keep separate histories")
    val second = browser.newTab()
    browser.visit("news.ycombinator.com", "Hacker News")
    println("  tab ${second.id} current: ${browser.activeTab.current?.url}, back possible: ${browser.activeTab.history.canGoBack}")
    browser.switchTo(1)
    println("  tab 1 current: ${browser.activeTab.current?.url}")

    println("-- global history")
    println("  recent: ${browser.recent(3).map { it.url }}")
    println("  search 'git': ${browser.search("git").map { it.url }}")
}
