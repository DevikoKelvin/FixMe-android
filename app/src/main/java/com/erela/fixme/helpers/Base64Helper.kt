package com.erela.fixme.helpers

object Base64Helper {
    fun isBase64Encoded(value: String): Boolean {
        val regex = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?\$".toRegex()
        return try {
            regex.containsMatchIn(value)
        } catch (exception: Exception) {
            exception.printStackTrace()
            false
        }
    }

    fun encodeBase64(string: String): String =
        java.util.Base64.getEncoder().encodeToString(string.toByteArray())

    fun decodeBase64(string: String): String = String(java.util.Base64.getDecoder().decode(string))
}