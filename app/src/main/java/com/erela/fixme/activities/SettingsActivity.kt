package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.BuildConfig
import com.erela.fixme.R
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivitySettingsBinding
import com.github.tutorialsandroid.appxupdater.AppUpdaterUtils
import com.github.tutorialsandroid.appxupdater.enums.AppUpdaterError
import com.github.tutorialsandroid.appxupdater.enums.UpdateFrom
import com.github.tutorialsandroid.appxupdater.objects.Update

class SettingsActivity : AppCompatActivity() {
    private val binding: ActivitySettingsBinding by lazy {
        ActivitySettingsBinding.inflate(layoutInflater)
    }
    private lateinit var currentAppVersion: String
    private var downloadLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()
    }

    @SuppressLint("SetTextI18n")
    private fun init() {
        binding.apply {
            backButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            currentAppVersion = BuildConfig.VERSION_NAME
            currentAppVersionText.text = "Current app version: $currentAppVersion"

            checkDownloadInstallButton.setOnClickListener {
                if (downloadLink != null) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW, downloadLink!!.toUri()
                        )
                    )
                } else {
                    val appUpdaterUtils = AppUpdaterUtils(this@SettingsActivity).also {
                        with(it) {
                            setUpdateFrom(UpdateFrom.GITHUB)
                            setGitHubUserAndRepo("DevikoKelvin", "FixMe-android")
                            withListener(object : AppUpdaterUtils.UpdateListener {
                                override fun onSuccess(
                                    update: Update?, isUpdateAvailable: Boolean?
                                ) {
                                    if (isUpdateAvailable == true) {
                                        checkDownloadInstallText.text =
                                            getString(R.string.download_update)
                                        updateAvailableStatus.visibility = View.VISIBLE
                                        currentAppVersionText.setTextColor(
                                            ContextCompat.getColor(
                                                this@SettingsActivity,
                                                R.color.custom_toast_font_failed
                                            )
                                        )
                                        newAppVersionText.text =
                                            "Detected new app version: ${update?.latestVersion}"
                                        newAppVersionText.visibility = View.VISIBLE
                                        downloadLink = update?.urlToDownload.toString()
                                    } else {
                                        if (currentAppVersion > update?.latestVersion!!) {
                                            CustomToast.getInstance(applicationContext)
                                                .setMessage("Your app version is higher than the latest version.")
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        this@SettingsActivity,
                                                        R.color.custom_toast_background_soft_blue
                                                    )
                                                )
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        this@SettingsActivity,
                                                        R.color.custom_toast_font_blue
                                                    )
                                                ).show()
                                        } else {
                                            CustomToast.getInstance(applicationContext)
                                                .setMessage("No update available. Your app is on the latest version!")
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        this@SettingsActivity,
                                                        R.color.custom_toast_background_soft_blue
                                                    )
                                                )
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        this@SettingsActivity,
                                                        R.color.custom_toast_font_blue
                                                    )
                                                ).show()
                                        }
                                        currentAppVersionText.setTextColor(
                                            ContextCompat.getColor(
                                                this@SettingsActivity,
                                                R.color.black
                                            )
                                        )
                                        checkDownloadInstallText.text =
                                            getString(R.string.check_for_update_now)
                                        updateAvailableStatus.visibility = View.GONE
                                        newAppVersionText.visibility = View.GONE
                                    }
                                }

                                override fun onFailed(error: AppUpdaterError?) {
                                    CustomToast.getInstance(applicationContext)
                                        .setMessage("Something went wrong, please try again.")
                                        .setFontColor(
                                            ContextCompat.getColor(
                                                this@SettingsActivity,
                                                R.color.custom_toast_font_failed
                                            )
                                        )
                                        .setBackgroundColor(
                                            ContextCompat.getColor(
                                                this@SettingsActivity,
                                                R.color.custom_toast_background_failed
                                            )
                                        ).show()
                                    currentAppVersionText.setTextColor(
                                        ContextCompat.getColor(
                                            this@SettingsActivity,
                                            R.color.black
                                        )
                                    )
                                    checkDownloadInstallText.text =
                                        getString(R.string.check_for_update_now)
                                    updateAvailableStatus.visibility = View.GONE
                                    newAppVersionText.visibility = View.GONE
                                    Log.e("ERROR Update", error.toString())
                                }
                            })
                        }
                    }
                    appUpdaterUtils.start()
                }
            }
        }
    }
}