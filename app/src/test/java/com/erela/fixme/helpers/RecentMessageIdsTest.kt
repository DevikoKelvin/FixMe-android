package com.erela.fixme.helpers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The same event arrives over ntfy and over FCM; whichever is first is shown and the other dropped.
 * Ids are random per test so the shared in-memory set cannot make one test depend on another.
 */
class RecentMessageIdsTest {
    private fun id() = java.util.UUID.randomUUID().toString()

    @Test
    fun the_first_copy_is_shown_and_the_second_is_dropped() {
        val id = id()

        assertTrue(RecentMessageIds.firstTime(id))
        assertFalse(RecentMessageIds.firstTime(id))
    }

    @Test
    fun different_events_do_not_hide_each_other() {
        assertTrue(RecentMessageIds.firstTime(id()))
        assertTrue(RecentMessageIds.firstTime(id()))
    }

    /** A server that predates msg_id sends none, and then nothing may be dropped on its account. */
    @Test
    fun a_missing_or_blank_id_is_never_a_duplicate() {
        assertTrue(RecentMessageIds.firstTime(null))
        assertTrue(RecentMessageIds.firstTime(null))
        assertTrue(RecentMessageIds.firstTime(""))
        assertTrue(RecentMessageIds.firstTime("   "))
    }

    /** Bounded: the oldest id is forgotten once more than 50 newer ones have been seen. */
    @Test
    fun it_forgets_the_oldest_after_fifty_newer_ones() {
        val oldest = id()
        RecentMessageIds.firstTime(oldest)

        repeat(60) { RecentMessageIds.firstTime(id()) }

        assertTrue("an evicted id counts as new again", RecentMessageIds.firstTime(oldest))
    }

    /** The shared topic carries everyone's events: ours must survive a burst of other people's. */
    @Test
    fun a_recent_id_survives_a_burst_smaller_than_the_window() {
        val mine = id()
        RecentMessageIds.firstTime(mine)

        repeat(20) { RecentMessageIds.firstTime(id()) }

        assertFalse(RecentMessageIds.firstTime(mine))
    }
}
