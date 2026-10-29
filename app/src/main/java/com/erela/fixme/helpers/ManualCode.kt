package com.erela.fixme.helpers

/**
 * A code typed in place of a scan, in the form the scanner would have produced. Null when nothing
 * was typed.
 *
 * UPPER-CASED, and that is not cosmetic: counter codes are `LDY-COUNTER-GA01`, compared exactly by
 * the server, and a phone keyboard offers lower case first - without this the fallback would refuse
 * the correct code and look broken. Garment patches are six digits, which this leaves alone. The
 * Compose app does the same.
 */
fun normalizeManualCode(raw: CharSequence?): String? =
    raw?.toString()?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
