package com.erela.fixme.dialogs

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import com.erela.fixme.BuildConfig
import com.erela.fixme.R
import com.erela.fixme.databinding.DialogChangelogBinding

/**
 * "What's new" shown once per installed version — on first launch after an update or a fresh
 * install.
 *
 * Text comes from app_versions, captured on the device by the *previous* build: every
 * checkUpdate response carries the notes for the version being offered, so rememberPending()
 * stores them against that version number and this dialog only uses them once the running
 * build actually matches. That is what makes the notes correspond to the installed version —
 * app_versions is unique per channel and gets edited in place, so asking the server after the
 * fact would return whatever release is current, not the one the user just installed.
 *
 * Falls back to R.string.changelog_current when no stored notes match, which covers fresh
 * installs and sideloaded APKs that never ran an update check.
 */
class ChangelogDialog(context: Context, private val changelog: String) : Dialog(context) {
    private val binding: DialogChangelogBinding by lazy {
        DialogChangelogBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        setCancelable(false)

        binding.apply {
            titleText.text = context.getString(R.string.changelog_title, BuildConfig.VERSION_NAME)
            changelogText.text = changelog
            dismissButton.setOnClickListener { dismiss() }
        }
    }

    companion object {
        // Separate from the "FixMe" preferences on purpose: UserDataHelper.purgeUserData()
        // clears those wholesale on logout, which would make the changelog reappear every
        // time someone signs out. "app_prefs" already exists in MainActivity and survives.
        private const val PREFS = "app_prefs"
        private const val KEY_LAST_SEEN_VERSION = "key.changelog.version"
        private const val KEY_PENDING_VERSION = "key.changelog.pending.version"
        private const val KEY_PENDING_ID = "key.changelog.pending.id"
        private const val KEY_PENDING_EN = "key.changelog.pending.en"

        /**
         * Records the notes an update check offered, tagged with the version they describe.
         * Safe to call on every check: if that version is never installed, the tag never
         * matches the running build and the notes are simply overwritten by the next check.
         */
        fun rememberPending(context: Context, versionName: String?, id: String?, en: String?) {
            if (versionName.isNullOrBlank()) return
            if (id.isNullOrBlank() && en.isNullOrBlank()) return

            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                putString(KEY_PENDING_VERSION, versionName)
                putString(KEY_PENDING_ID, id)
                putString(KEY_PENDING_EN, en)
            }
        }

        /**
         * Shows the dialog only when the installed versionName differs from the last one
         * acknowledged on this device. Absent (fresh install) counts as different.
         *
         * Marks as seen when shown rather than when dismissed: the dialog is not cancelable,
         * but the activity can still be torn down under it (rotation, process death), and
         * re-showing on every launch until a dismiss lands is worse than a missed read.
         */
        fun showIfVersionIsNew(context: Context) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

            if (prefs.getString(KEY_LAST_SEEN_VERSION, null) == BuildConfig.VERSION_NAME) return

            // No text to show is not the same as nothing to record: mark the version seen
            // either way, so a release with an empty changelog does not re-check every launch.
            val changelog = resolveChangelog(context, prefs).takeIf { it.isNotBlank() }
            prefs.edit { putString(KEY_LAST_SEEN_VERSION, BuildConfig.VERSION_NAME) }
            if (changelog == null) return

            ChangelogDialog(context, changelog)
                .takeIf { it.window != null }
                ?.show()
        }

        private fun resolveChangelog(context: Context, prefs: SharedPreferences): String {
            if (prefs.getString(KEY_PENDING_VERSION, null) == BuildConfig.VERSION_NAME) {
                // Device locale decides, the same probe the rest of this app uses. Falls
                // through to the other language when the preferred column was left blank —
                // in practice only one of the two is filled in for a given release.
                val indonesian = context.getString(R.string.lang) == "in"

                val preferred = prefs.getString(
                    if (indonesian) KEY_PENDING_ID else KEY_PENDING_EN, null
                )?.takeIf { it.isNotBlank() }

                val other = prefs.getString(
                    if (indonesian) KEY_PENDING_EN else KEY_PENDING_ID, null
                )?.takeIf { it.isNotBlank() }

                (preferred ?: other)?.let { return it }
            }

            return context.getString(R.string.changelog_current)
        }
    }
}
