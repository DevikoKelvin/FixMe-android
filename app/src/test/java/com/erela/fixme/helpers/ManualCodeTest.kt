package com.erela.fixme.helpers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** A typed code must reach the server exactly as a scanned one would have. */
class ManualCodeTest {
    @Test
    fun a_counter_code_typed_in_lower_case_is_upper_cased() {
        assertEquals("LDY-COUNTER-GA01", normalizeManualCode("ldy-counter-ga01"))
    }

    @Test
    fun surrounding_spaces_are_dropped() {
        assertEquals("LDY-COUNTER-GA01", normalizeManualCode("  Ldy-Counter-Ga01 \n"))
    }

    @Test
    fun a_six_digit_garment_patch_is_left_alone() {
        assertEquals("000123", normalizeManualCode("000123"))
    }

    @Test
    fun nothing_typed_is_not_a_code() {
        assertNull(normalizeManualCode(null))
        assertNull(normalizeManualCode(""))
        assertNull(normalizeManualCode("   "))
    }
}
