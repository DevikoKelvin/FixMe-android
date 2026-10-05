package com.erela.fixme

import android.content.pm.ApplicationInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What separates a release build from a debug one, checked on the APK as installed.
 *
 * Runs against either build type; see testBuildType in app/build.gradle.
 */
@RunWith(AndroidJUnit4::class)
class BuildVariantTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationInfo

    @Test
    fun installs_under_its_build_types_package() {
        // Every channel ships as com.erela.fixme, which is what lets a phone switch channels by
        // updating. Debug keeps a suffix so it installs beside them rather than over them.
        assertEquals(if (BuildConfig.DEBUG) "com.erela.fixme.debug" else "com.erela.fixme", app.packageName)
    }

    @Test
    fun only_debug_builds_are_debuggable() {
        // A debuggable release lets anyone holding the phone read its saved login token over USB.
        assertEquals(BuildConfig.DEBUG, (app.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
    }

    @Test
    fun release_talks_to_the_public_server() {
        // A LAN address only resolves inside the office, so phones out in the field would get nothing.
        // Debug builds use the office dev server on purpose, so there it passes without checking:
        // not assumeFalse(DEBUG), which Android Studio reports as a failed test.
        assertTrue(BuildConfig.BASE_URL, BuildConfig.DEBUG || !BuildConfig.BASE_URL.contains("://192.168."))
    }
}
