package com.erela.fixme.helpers

object VersionHelper {
    fun compareVersions(version1: String?, version2: String?): Int {
        if (version1 == null || version2 == null) {
            return 0
        }

        // Remove "v" prefix if it exists and trim whitespace
        val v1 = version1.trim().removePrefix("v")
        val v2 = version2.trim().removePrefix("v")

        val parts1 = v1.split('.')
        val parts2 = v2.split('.')

        val length = if (parts1.size > parts2.size) parts1.size else parts2.size

        for (i in 0 until length) {
            val part1 = parts1.getOrNull(i) ?: "0"
            val part2 = parts2.getOrNull(i) ?: "0"

            val num1Str = part1.filter { it.isDigit() }
            val num2Str = part2.filter { it.isDigit() }

            val num1 = num1Str.toIntOrNull() ?: 0
            val num2 = num2Str.toIntOrNull() ?: 0

            if (num1 > num2) return 1
            if (num1 < num2) return -1

            val suffix1 = part1.filter { it.isLetter() }
            val suffix2 = part2.filter { it.isLetter() }
            
            val suffixComparison = suffix1.compareTo(suffix2)
            if (suffixComparison != 0) {
                return if (suffixComparison > 0) 1 else -1
            }
        }

        return 0
    }
}
