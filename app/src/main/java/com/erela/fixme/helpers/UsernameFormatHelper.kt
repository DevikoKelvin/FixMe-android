package com.erela.fixme.helpers

object UsernameFormatHelper {
    fun getRealUsername(username: String): String = username.subSequence(0, 1).toString()
        .uppercase() + username.subSequence(
        1,
        username.length
    ).toString().lowercase()
}