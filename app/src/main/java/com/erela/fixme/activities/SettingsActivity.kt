package com.erela.fixme.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.io.File

class SettingsActivity : AppCompatActivity() {
    private val binding: ActivitySettingsBinding by lazy {
        ActivitySettingsBinding.inflate(layoutInflater)
    }
    private lateinit var currentAppVersion: String
    private var newAppVersion: String? = null
    private var downloadLink: String? = null
    private var downloadProgress: Int = 0
    private var downloadId: Long = 0
    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cursor.moveToFirst()) {
                    val statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusColumnIndex != -1) {
                        val status = cursor.getInt(statusColumnIndex)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            showDownloadProgressNotification(downloadId.toInt(), 100)
                            cancelNotification(downloadId.toInt())
                            val uriColumnIndex =
                                cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            if (uriColumnIndex != -1) {
                                val fileUriString = cursor.getString(uriColumnIndex)
                                if (fileUriString != null) {
                                    val fileUri = Uri.parse(fileUriString)
                                    installApk(fileUri)
                                } else {
                                    Log.e("DownloadManager", "COLUMN_LOCAL_URI is null")
                                    showDownloadFailedToast()
                                    cancelNotification(downloadId.toInt())
                                }
                            } else {
                                Log.e("DownloadManager", "COLUMN_LOCAL_URI not found")
                                showDownloadFailedToast()
                                cancelNotification(downloadId.toInt())
                            }
                        } else if (status == DownloadManager.STATUS_RUNNING) {
                            val bytesDownloadedIndex =
                                cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val bytesTotalIndex =
                                cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                                val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                                val bytesTotal = cursor.getLong(bytesTotalIndex)
                                if (bytesTotal > 0) {
                                    downloadProgress =
                                        ((bytesDownloaded * 100) / bytesTotal).toInt()
                                    showDownloadProgressNotification(
                                        downloadId.toInt(),
                                        downloadProgress
                                    )
                                }
                            }
                        } else {
                            Log.e("DownloadManager", "Download not successful, status: $status")
                            showDownloadFailedToast()
                            cancelNotification(downloadId.toInt())
                        }
                    } else {
                        Log.e("DownloadManager", "COLUMN_STATUS not found")
                        showDownloadFailedToast()
                        cancelNotification(downloadId.toInt())
                    }
                }
                cursor.close()
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
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
                    startDownload(downloadLink!!)
                } else {
                    loadingBar.visibility = View.VISIBLE
                    val handler = Handler(Looper.getMainLooper())
                    val runnable = object : Runnable {
                        var count = 1
                        override fun run() {
                            when (count) {
                                1 -> checkDownloadInstallText.text = "Checking update."
                                2 -> checkDownloadInstallText.text = "Checking update.."
                                3 -> checkDownloadInstallText.text = "Checking update..."
                            }
                            count = if (count < 3) count + 1 else 1
                            handler.postDelayed(this, 500)
                        }
                    }

                    handler.post(runnable)
                    val appUpdaterUtils = AppUpdaterUtils(this@SettingsActivity).also {
                        with(it) {
                            setUpdateFrom(UpdateFrom.GITHUB)
                            setGitHubUserAndRepo("DevikoKelvin", "FixMe-android")
                            withListener(object : AppUpdaterUtils.UpdateListener {
                                override fun onSuccess(
                                    update: Update?, isUpdateAvailable: Boolean?
                                ) {
                                    loadingBar.visibility = View.GONE
                                    handler.removeCallbacks(runnable)
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
                                        newAppVersion = update?.latestVersion
                                        newAppVersionText.text =
                                            "Detected new app version: ${update?.latestVersion}"
                                        newAppVersionText.visibility = View.VISIBLE
                                        downloadLink = update?.urlToDownload.toString().replace(
                                            "latest",
                                            "download/v${update?.latestVersion}/Erela_FixMe_prerelease_v${update?.latestVersion}.apk"
                                        )
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
                                    loadingBar.visibility = View.GONE
                                    handler.removeCallbacks(runnable)
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

    private fun showDownloadFailedToast() {
        CustomToast.getInstance(applicationContext)
            .setMessage("Download failed.")
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
    }

    private fun startDownload(url: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("FixMe Updates")
            .setDescription("Please wait while the update is downloading...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "FixMe Updates.apk"
            )
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
        CustomToast.getInstance(applicationContext)
            .setMessage("Downloading update...")
            .setFontColor(
                ContextCompat.getColor(
                    this@SettingsActivity,
                    R.color.custom_toast_font_blue
                )
            )
            .setBackgroundColor(
                ContextCompat.getColor(
                    this@SettingsActivity,
                    R.color.custom_toast_background_soft_blue
                )
            ).show()
        showDownloadProgressNotification(downloadId.toInt(), 0)
    }

    private fun showDownloadProgressNotification(notificationId: Int, progress: Int) {
        val builder = NotificationCompat.Builder(this, "FixMe Download Channel")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Erela_FixMe_prerelease_v${newAppVersion}")
            .setContentText("Download in progress")
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setOnlyAlertOnce(true)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@SettingsActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(notificationId, builder.build())
        }
    }

    private fun cancelNotification(notificationId: Int) {
        with(NotificationManagerCompat.from(this)) {
            cancel(notificationId)
        }
    }

    private fun installApk(uri: Uri) {
        try {
            val downloadedFile = File(uri.path!!)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    FileProvider.getUriForFile(
                        this@SettingsActivity,
                        "${BuildConfig.APPLICATION_ID}.provider",
                        downloadedFile
                    ),
                    "application/vnd.android.package-archive"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (installIntent.resolveActivity(packageManager) != null)
                startActivity(installIntent)
            else
                println("No activity found to handle APK installation")
        } catch (e: Exception) {
            Log.e("InstallApk", "Error installing APK", e)
            CustomToast.getInstance(applicationContext)
                .setMessage("Failed to install APK.")
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
        }
    }
}