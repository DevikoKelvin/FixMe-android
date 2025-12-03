package com.erela.fixme.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.BuildConfig
import com.erela.fixme.R
import com.erela.fixme.bottom_sheets.UserInfoBottomSheet
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityMainBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.erela.fixme.dialogs.UpdateAvailableDialog
import com.erela.fixme.helpers.NotificationsHelper
import com.erela.fixme.helpers.PermissionHelper
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.UserData
import com.erela.fixme.services.NotificationService
import com.github.tutorialsandroid.appxupdater.AppUpdaterUtils
import com.github.tutorialsandroid.appxupdater.enums.AppUpdaterError
import com.github.tutorialsandroid.appxupdater.enums.UpdateFrom
import com.github.tutorialsandroid.appxupdater.objects.Update
import java.io.File
import java.time.LocalDateTime

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private val userData: UserData by lazy {
        UserDataHelper(this@MainActivity).getUserData()
    }
    private var downloadProgress: Int = 0
    private var downloadId: Long = 0
    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
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
                                    val fileUri = fileUriString.toUri()
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
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        /*createNotificationChannel()*/
        init()
        checkNewUpdate()
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

    override fun onPause() {
        super.onPause()
        NotificationsHelper.disconnectPusher()
    }

    @SuppressLint("SetTextI18n", "InlinedApi")
    private fun init() {
        binding.apply {
            if (!PermissionHelper.isPermissionGranted(
                    this@MainActivity, PermissionHelper.POST_NOTIFICATIONS
                )
            ) {
                PermissionHelper.requestPermission(
                    this@MainActivity, arrayOf(PermissionHelper.POST_NOTIFICATIONS),
                    PermissionHelper.REQUEST_CODE_NOTIFICATION
                )
            }

            if (!PermissionHelper.isPermissionGranted(
                    this@MainActivity, PermissionHelper.INSTALL_PACKAGES
                )
            ) {
                PermissionHelper.requestPermission(
                    this@MainActivity, arrayOf(PermissionHelper.INSTALL_PACKAGES),
                    PermissionHelper.REQUEST_INSTALL_PACKAGES
                )
            }

            if (!isFinishing) {
                runOnUiThread {
                    if (!isServiceRunning(NotificationService::class.java)) {
                        Intent(this@MainActivity, NotificationService::class.java).also {
                            startForegroundService(it)
                        }
                    }

                    /*if (UserDataHelper(this@MainActivity).getNotification())
                        NotificationsHelper.receiveNotifications(applicationContext, userData)*/

                    /*PushNotifications.start(
                        applicationContext,
                        "66b9148d-50f4-4114-b258-e9e9485ce75c"
                    )*/
                }
            }

            when (userData.privilege) {
                0 -> {
                    privilegeContainer.background = ContextCompat.getDrawable(
                        applicationContext, R
                            .drawable.toolbar_background_owner
                    )
                    privilegeText.text = if (getString(R.string.lang) == "in")
                        "Pemilik"
                    else
                        "Owner"
                    privilegeText.setTextColor(
                        ContextCompat.getColor(
                            applicationContext, R.color.white
                        )
                    )
                }

                1 -> {
                    privilegeContainer.background = ContextCompat.getDrawable(
                        applicationContext, R
                            .drawable.toolbar_background_manager
                    )
                    privilegeText.text = if (getString(R.string.lang) == "in")
                        "Manajer/Asisten/Admin"
                    else
                        "Manager/Assistant/Admin"
                    privilegeText.setTextColor(
                        ContextCompat.getColor(
                            applicationContext, R.color.white
                        )
                    )
                }

                2 -> {
                    privilegeContainer.background = ContextCompat.getDrawable(
                        applicationContext, R
                            .drawable.toolbar_background_supervisor
                    )
                    privilegeText.text = "Supervisor"
                    privilegeText.setTextColor(
                        ContextCompat.getColor(
                            applicationContext, R.color.black
                        )
                    )
                }

                3 -> {
                    privilegeContainer.background = ContextCompat.getDrawable(
                        applicationContext, R
                            .drawable.toolbar_background_technician
                    )
                    privilegeText.text = if (getString(R.string.lang) == "in")
                        "Teknisi"
                    else
                        "Technician"
                    privilegeText.setTextColor(
                        ContextCompat.getColor(
                            applicationContext, R.color.black
                        )
                    )
                }

                4 -> {
                    privilegeContainer.background = ContextCompat.getDrawable(
                        applicationContext, R
                            .drawable.toolbar_background_staff
                    )
                    privilegeText.text = if (getString(R.string.lang) == "in")
                        "Staf/Pelapor"
                    else
                        "Staff/Reporter"
                    privilegeText.setTextColor(
                        ContextCompat.getColor(
                            applicationContext, R.color.black
                        )
                    )
                }
            }
            val currentDateTime = LocalDateTime.now()
            greetingText.text =
                if (getString(R.string.lang) == "in")
                    "Selamat ${
                        when (currentDateTime.hour) {
                            in 0..11 -> "pagi,"
                            in 12..18 -> "siang,"
                            else -> "malam,"
                        }
                    }"
                else
                    "Good ${
                        when (currentDateTime.hour) {
                            in 0..11 -> "morning,"
                            in 12..18 -> "afternoon,"
                            else -> "evening,"
                        }
                    }"
            usernameText.text = "${userData.name.trimEnd()}!"
            userInfoButton.setOnClickListener {
                UserInfoBottomSheet(this@MainActivity).also {
                    with(it) {
                        if (window != null)
                            show()
                    }
                }
            }

            notificationButton.setOnClickListener {
                notificationAnimation.playAnimation()
                startActivity(Intent(this@MainActivity, NotificationActivity::class.java))
            }

            changePasswordMenu.setOnClickListener {
                startActivity(Intent(this@MainActivity, ChangePasswordActivity::class.java))
            }

            makeSubmissionMenu.setOnClickListener {
                startActivity(Intent(this@MainActivity, SubmissionFormActivity::class.java))
            }

            submissionListMenu.setOnClickListener {
                startActivity(Intent(this@MainActivity, SubmissionListActivity::class.java))
            }

            settingsMenu.setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }

            logOutButton.setOnClickListener {
                val confirmationDialog =
                    ConfirmationDialog(
                        this@MainActivity,
                        if (getString(R.string.lang) == "in")
                            "Apakah Anda yakin ingin keluar?"
                        else
                            "Are you sure you want to log out?",
                        if (getString(R.string.lang) == "in") "Ya" else "Yes"
                    ).also {
                        with(it) {
                            setConfirmationDialogListener(object :
                                ConfirmationDialog.ConfirmationDialogListener {
                                override fun onConfirm() {
                                    Intent(
                                        this@MainActivity,
                                        NotificationService::class.java
                                    ).also { intent ->
                                        stopService(intent)
                                    }
                                    UserDataHelper(this@MainActivity).purgeUserData()
                                    Toast.makeText(
                                        this@MainActivity,
                                        if (getString(R.string.lang) == "in")
                                            "Berhasil keluar!"
                                        else
                                            "Successfully logged out!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            LoginActivity::class.java
                                        )
                                    ).also {
                                        finish()
                                    }
                                }
                            })
                        }
                    }

                if (confirmationDialog.window != null)
                    confirmationDialog.show()
            }
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun checkNewUpdate() {
        AppUpdaterUtils(this@MainActivity).also {
            with(it) {
                setUpdateFrom(UpdateFrom.GITHUB)
                setGitHubUserAndRepo("DevikoKelvin", "FixMe-android")
                withListener(object : AppUpdaterUtils.UpdateListener {
                    override fun onSuccess(update: Update?, isUpdateAvailable: Boolean?) {
                        if (isUpdateAvailable == true) {
                            val dialog = UpdateAvailableDialog(
                                this@MainActivity,
                                update?.urlToDownload.toString().replace(
                                    "latest",
                                    "download/v${update?.latestVersion}/Erela_FixMe_prerelease_v${update?.latestVersion}.apk"
                                )
                            ).also { updateDialog ->
                                with(updateDialog) {
                                    setOnDownloadListener(object :
                                        UpdateAvailableDialog.OnDownloadListener {
                                        override fun onDownload(url: String) {
                                            startDownload(url)
                                        }
                                    })
                                }
                            }

                            if (dialog.window != null)
                                dialog.show()
                        } else
                            return
                    }

                    override fun onFailed(error: AppUpdaterError?) {
                        Log.e(
                            "ERROR Update Check",
                            "Can't retrieve update channel | ERROR ${error.toString()}"
                        )
                    }
                })
            }
        }.start()
    }

    private fun showDownloadFailedToast() {
        CustomToast.getInstance(applicationContext)
            .setMessage(
                if (getString(R.string.lang) == "in")
                    "Gagal mengunduh pembaruan."
                else
                    "Download failed."
            )
            .setFontColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.custom_toast_font_failed
                )
            )
            .setBackgroundColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.custom_toast_background_failed
                )
            ).show()
    }

    private fun startDownload(url: String) {
        val request = DownloadManager.Request(url.toUri())
            .setTitle(
                if (getString(R.string.lang) == "in")
                    "Pembaruan FixMe"
                else
                    "FixMe Updates"
            )
            .setDescription(
                if (getString(R.string.lang) == "in")
                    "Mohon tunggu, pembaruan sedang diunduh..."
                else
                    "Please wait while the update is downloading..."
            )
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "FixMe Updates"
            )
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)
        CustomToast.getInstance(applicationContext)
            .setMessage(
                if (getString(R.string.lang) == "in")
                    "Mengunduh pembaruan..."
                else
                    "Downloading update..."
            )
            .setFontColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.custom_toast_font_blue
                )
            )
            .setBackgroundColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.custom_toast_background_soft_blue
                )
            ).show()
        showDownloadProgressNotification(downloadId.toInt(), 0)
    }

    private fun installApk(uri: Uri) {
        try {
            val downloadedFile = File(uri.path!!)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    FileProvider.getUriForFile(
                        this@MainActivity,
                        "${BuildConfig.APPLICATION_ID}.provider",
                        downloadedFile
                    ),
                    "application/vnd.android.package-archive"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(installIntent)
            } catch (ex: ActivityNotFoundException) {
                CustomToast.getInstance(applicationContext)
                    .setMessage(
                        if (getString(R.string.lang) == "in")
                            "Tidak ada aktivitas yang ditemukan untuk menangani instalasi APK."
                        else
                            "No activity found to handle APK installation."
                    )
                    .setFontColor(
                        ContextCompat.getColor(
                            this@MainActivity,
                            R.color.custom_toast_font_failed
                        )
                    )
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            this@MainActivity,
                            R.color.custom_toast_background_failed
                        )
                    ).show()
                ex.printStackTrace()
            }
        } catch (e: Exception) {
            Log.e("InstallApk", "Error installing APK", e)
            CustomToast.getInstance(applicationContext)
                .setMessage(
                    if (getString(R.string.lang) == "in")
                        "Gagal menginstal APK."
                    else
                        "Failed to install APK."
                )
                .setFontColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.custom_toast_font_failed
                    )
                )
                .setBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        R.color.custom_toast_background_failed
                    )
                ).show()
        }
    }

    private fun showDownloadProgressNotification(notificationId: Int, progress: Int) {
        val builder = NotificationCompat.Builder(this, "FixMe Download Channel")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(
                if (getString(R.string.lang) == "in")
                    "Pembaruan FixMe"
                else
                    "FixMe Updates"
            )
            .setContentText(
                if (getString(R.string.lang) == "in")
                    "Sedang mengunduh..."
                else
                    "Download in progress"
            )
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setOnlyAlertOnce(true)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
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
}