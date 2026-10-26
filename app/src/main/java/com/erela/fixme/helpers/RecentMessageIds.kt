package com.erela.fixme.helpers

/**
 * Remembers the notifications this process has already shown, by the server's `msg_id`.
 *
 * WHY. The same event now arrives twice: over ntfy (SseService) and over FCM (FCMService). Whichever
 * lands first is shown and the other is dropped, so the user sees one notification either way.
 *
 * It replaces comparing the case id (`FCMService.lastNotificationId`), which cannot tell two events
 * apart when they share one: AC and laundry notifications are sent with case id 0, so the old check
 * hid every one of them after the first. `msg_id` is a fresh UUID per event. A server that predates
 * it sends none, and callers then fall back to the old comparison.
 *
 * In memory, 50 entries: the two copies of an event arrive seconds apart, so there is nothing to
 * survive a process restart for.
 */
object RecentMessageIds {
    private const val MAX = 50

    private val seen = object : LinkedHashMap<String, Unit>(MAX, 0.75f, false) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Unit>?) = size > MAX
    }

    /** True the first time [id] is offered, false for every repeat. A blank id is always "first". */
    @Synchronized
    fun firstTime(id: String?): Boolean {
        if (id.isNullOrBlank()) return true
        return seen.put(id, Unit) == null
    }
}
