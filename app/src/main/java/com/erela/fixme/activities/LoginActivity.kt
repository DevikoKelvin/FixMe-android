package com.erela.fixme.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.erela.fixme.helpers.enableEdgeToEdgeOpaqueNav
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.BuildConfig
import com.erela.fixme.R
import com.erela.fixme.bottom_sheets.ChannelPickerBottomSheet
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityLoginBinding
import com.erela.fixme.dialogs.UpdateAvailableDialog
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.GenericSimpleResponse
import com.erela.fixme.objects.LoginResponse
import com.erela.fixme.objects.UpdateCheckResponse
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class LoginActivity : AppCompatActivity() {
    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }
    private var lockoutTimer: CountDownTimer? = null
    private var newAppVersion: String? = null
    private var downloadProgress: Int = 0
    private var downloadId: Long = 0
    private var pendingDeleteApkFile: File? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdgeOpaqueNav()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            // Use the larger of the navigation bar or the keyboard height as the bottom padding.
            // This lifts the content above the keyboard when it appears on small screens.
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }

        init()
    }

    override fun dispatchTouchEvent(motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            val view: View? = currentFocus
            if (view is TextInputEditText || view is EditText) {
                val rect = Rect()
                view.getGlobalVisibleRect(rect)
                if (!rect.contains(motionEvent.rawX.toInt(), motionEvent.rawY.toInt())) {
                    view.clearFocus()
                    val inputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(motionEvent)
    }

    override fun onDestroy() {
        super.onDestroy()
        lockoutTimer?.cancel()
        unregisterReceiver(onDownloadComplete)
    }

    override fun onResume() {
        super.onResume()
        pendingDeleteApkFile?.let { file ->
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(
                    "APKCleanup",
                    "Downloaded APK ${if (deleted) "deleted" else "delete failed"}: ${file.name}"
                )
            }
            pendingDeleteApkFile = null
        }
    }

    override fun onPause() {
        super.onPause()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun init() {
        binding.apply {
            if (UserDataHelper(this@LoginActivity).isUserDataExist()) {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java)).also {
                    finish()
                }
            }

            val channel =
                if (
                    (BuildConfig.VERSION_CHANNEL == "release" || BuildConfig.VERSION_CHANNEL == "beta_prerelease")
                    && BuildConfig.BUILD_TYPE == "release"
                )
                    "${BuildConfig.VERSION_CHANNEL}"
                else
                    "${BuildConfig.VERSION_CHANNEL}_${BuildConfig.BUILD_TYPE}"
            val appVersionText =
                "${BuildConfig.VERSION_NAME}.${BuildConfig.VERSION_CODE}.${channel}.${BuildConfig.BUILD_TIMESTAMP}"
            versionText.text = if (getString(R.string.lang) == "in")
                "Versi $appVersionText"
            else
                "Version $appVersionText"

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

            loginButton.setOnClickListener {
                loginText.visibility = View.GONE
                loadingBar.visibility = View.VISIBLE
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                if (usernameField.text.isNullOrEmpty() || passwordField.text.isNullOrEmpty()) {
                    loginText.visibility = View.VISIBLE
                    loadingBar.visibility = View.GONE
                    CustomToast.getInstance(applicationContext)
                        .setMessage(
                            if (getString(R.string.lang) == "in")
                                "Mohon isi semua kolom."
                            else
                                "Please fill in all fields."
                        )
                        .setFontColor(
                            ContextCompat.getColor(
                                this@LoginActivity,
                                R.color.custom_toast_font_failed
                            )
                        )
                        .setBackgroundColor(
                            ContextCompat.getColor(
                                this@LoginActivity,
                                R.color.custom_toast_background_failed
                            )
                        ).show()
                    if (usernameField.text.isNullOrEmpty()) {
                        usernameFieldLayout.error = if (getString(R.string.lang) == "in")
                            "Username tidak boleh kosong"
                        else
                            "Username cannot be empty"
                    } else {
                        usernameFieldLayout.error = null
                    }
                    if (passwordField.text.isNullOrEmpty()) {
                        passwordFieldLayout.error = if (getString(R.string.lang) == "in")
                            "Kata sandi tidak boleh kosong"
                        else
                            "Password cannot be empty"
                    } else {
                        passwordFieldLayout.error = null
                    }
                } else {
                    checkLogin(
                        usernameField.text.toString().trim(),
                        passwordField.text.toString().trim()
                    )
                }
            }

            // Add keyboard action listeners for Enter key
            usernameField.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                ) {
                    passwordField.requestFocus()
                    true
                } else {
                    false
                }
            }

            passwordField.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    loginButton.performClick()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun checkLogin(username: String, password: String) {
        binding.apply {
            usernameFieldLayout.error = null
            passwordFieldLayout.error = null
            loginText.visibility = View.GONE
            loadingBar.visibility = View.VISIBLE

            try {
                val deviceId = android.provider.Settings.Secure.getString(
                    contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
                InitAPI.getEndpoint.login(username, password, deviceId)
                    .enqueue(object : Callback<LoginResponse> {
                        override fun onResponse(
                            call: Call<LoginResponse?>,
                            response: Response<LoginResponse?>
                        ) {
                            loginText.visibility = View.VISIBLE
                            loadingBar.visibility = View.GONE
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    val result = response.body()
                                    val name = result?.name
                                    when (result?.code) {
                                        1 -> {
                                            CustomToast.getInstance(applicationContext)
                                                .setMessage(
                                                    if (getString(R.string.lang) == "in")
                                                        "Berhasil Masuk!"
                                                    else
                                                        "Login Successful!"
                                                )
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        this@LoginActivity,
                                                        R.color.custom_toast_font_success
                                                    )
                                                )
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        this@LoginActivity,
                                                        R.color.custom_toast_background_success
                                                    )
                                                ).show()
                                            UserDataHelper(this@LoginActivity)
                                                .setUserData(
                                                    result.userId!!,
                                                    result.starConnectId!!,
                                                    username,
                                                    name!!,
                                                    result.privilege!!,
                                                    result.deptId!!,
                                                    result.deptName!!,
                                                    result.subDept!!,
                                                    result.email ?: ""
                                                )
                                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                                if (!task.isSuccessful) {
                                                    Log.e(
                                                        "LoginActivity",
                                                        "Fetching FCM registration token failed",
                                                        task.exception
                                                    )
                                                    return@addOnCompleteListener
                                                }
                                                val token = task.result
                                                Log.e("LoginActivity", "FCM Token: $token")

                                                InitAPI.getEndpoint.updateFcmToken(
                                                    result.userId,
                                                    token
                                                ).enqueue(object : Callback<GenericSimpleResponse> {
                                                    override fun onResponse(
                                                        call: Call<GenericSimpleResponse>,
                                                        response: Response<GenericSimpleResponse>
                                                    ) {
                                                        if (response.isSuccessful) {
                                                            Log.e(
                                                                "LoginActivity",
                                                                "FCM token updated successfully"
                                                            )
                                                        } else {
                                                            Log.e(
                                                                "LoginActivity",
                                                                "Failed to update FCM token: ${response.code()} ${response.message()}"
                                                            )
                                                        }
                                                    }

                                                    override fun onFailure(
                                                        call: Call<GenericSimpleResponse>,
                                                        t: Throwable
                                                    ) {
                                                        Log.e(
                                                            "LoginActivity",
                                                            "Failed to update FCM token",
                                                            t
                                                        )
                                                    }
                                                })
                                            }
                                            val sharedPreferences = getSharedPreferences(
                                                "app_prefs",
                                                MODE_PRIVATE
                                            )
                                            sharedPreferences.edit {
                                                putBoolean("first_login", true)
                                            }
                                            Handler(mainLooper).postDelayed({
                                                CustomToast.getInstance(applicationContext)
                                                    .setMessage(
                                                        if (getString(R.string.lang) == "in")
                                                            "Selamat datang, $name!"
                                                        else
                                                            "Welcome, $name!"
                                                    )
                                                    .setFontColor(
                                                        ContextCompat.getColor(
                                                            this@LoginActivity,
                                                            R.color.custom_toast_font_normal_soft_gray
                                                        )
                                                    )
                                                    .setBackgroundColor(
                                                        ContextCompat.getColor(
                                                            this@LoginActivity,
                                                            R.color.custom_toast_background_normal_dark_gray
                                                        )
                                                    ).show()
                                                startActivity(
                                                    Intent(
                                                        this@LoginActivity,
                                                        MainActivity::class.java
                                                    )
                                                ).also {
                                                    finish()
                                                }
                                            }, 2000)
                                        }

                                        2 -> {
                                            Log.e("LoginActivity", "Error: ${response.message()}")
                                            CustomToast.getInstance(applicationContext)
                                                .setMessage(
                                                    if (getString(R.string.lang) == "in")
                                                        "Username dan kata sandi wajib diisi."
                                                    else
                                                        "Username and password are required."
                                                )
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        this@LoginActivity,
                                                        R.color.custom_toast_font_warning
                                                    )
                                                )
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        this@LoginActivity,
                                                        R.color.custom_toast_background_warning
                                                    )
                                                ).show()
                                        }

                                        3 -> {
                                            Log.e("LoginActivity", "Error: ${response.message()}")
                                            CustomToast.getInstance(applicationContext)
                                                .setMessage(
                                                    if (getString(R.string.lang) == "in")
                                                        "Pengguna tidak ditemukan. Silakan hubungi bagian IT."
                                                    else
                                                        "User not found. Please contact the IT department."
                                                )
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        this@LoginActivity,
                                                        R.color.custom_toast_font_warning
                                                    )
                                                )
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        this@LoginActivity,
                                                        R.color.custom_toast_background_warning
                                                    )
                                                ).show()
                                        }

                                        4 -> {
                                            CustomToast.getInstance(applicationContext)
                                                .setMessage(
                                                    if (getString(R.string.lang) == "in")
                                                        "Kata sandi salah. Silakan coba lagi!"
                                                    else
                                                        "Wrong password. Please try again!"
                                                )
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        this@LoginActivity,
                                                        R.color.custom_toast_font_failed
                                                    )
                                                )
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        this@LoginActivity,
                                                        R.color.custom_toast_background_failed
                                                    )
                                                ).show()
                                            passwordFieldLayout.error =
                                                if (getString(R.string.lang) == "in")
                                                    "Kata sandi salah"
                                                else
                                                    "Wrong password"
                                        }

                                        -429 -> {
                                            val langIn = getString(R.string.lang) == "in"
                                            val lockSecs = (result.lockoutSeconds ?: 300).toLong()
                                            loginButton.isEnabled = false
                                            lockoutTimer?.cancel()
                                            lockoutTimer =
                                                object : CountDownTimer(lockSecs * 1000L, 1000L) {
                                                    override fun onTick(ms: Long) {
                                                        val s = (ms / 1000).toInt()
                                                        val txt =
                                                            if (s >= 60) "${s / 60}m ${s % 60}s" else "${s}s"
                                                        loginText.text =
                                                            if (langIn) "Coba lagi dalam $txt" else "Try again in $txt"
                                                    }

                                                    override fun onFinish() {
                                                        loginText.text =
                                                            if (langIn) "Masuk" else "Sign In"
                                                        loginButton.isEnabled = true
                                                    }
                                                }.start()
                                            CustomToast.getInstance(applicationContext)
                                                .setMessage(
                                                    if (langIn)
                                                        "Terlalu banyak percobaan login. Silakan tunggu."
                                                    else
                                                        "Too many login attempts. Please wait."
                                                )
                                                .setFontColor(
                                                    ContextCompat.getColor(
                                                        this@LoginActivity,
                                                        R.color.custom_toast_font_failed
                                                    )
                                                )
                                                .setBackgroundColor(
                                                    ContextCompat.getColor(
                                                        this@LoginActivity,
                                                        R.color.custom_toast_background_failed
                                                    )
                                                ).show()
                                        }
                                    }
                                } else {
                                    Log.e("LoginActivity", "Error: ${response.message()}")
                                    CustomToast.getInstance(applicationContext)
                                        .setMessage(
                                            if (getString(R.string.lang) == "in")
                                                "Terjadi kesalahan, silakan coba lagi."
                                            else
                                                "Something went wrong, please try again."
                                        )
                                        .setFontColor(
                                            ContextCompat.getColor(
                                                this@LoginActivity,
                                                R.color.custom_toast_font_failed
                                            )
                                        )
                                        .setBackgroundColor(
                                            ContextCompat.getColor(
                                                this@LoginActivity,
                                                R.color.custom_toast_background_failed
                                            )
                                        ).show()
                                }
                            } else {
                                Log.e("LoginActivity", "Error: ${response.message()}")
                                CustomToast.getInstance(applicationContext)
                                    .setMessage(
                                        if (getString(R.string.lang) == "in")
                                            "Terjadi kesalahan, silakan coba lagi."
                                        else
                                            "Something went wrong, please try again."
                                    )
                                    .setFontColor(
                                        ContextCompat.getColor(
                                            this@LoginActivity,
                                            R.color.custom_toast_font_failed
                                        )
                                    )
                                    .setBackgroundColor(
                                        ContextCompat.getColor(
                                            this@LoginActivity,
                                            R.color.custom_toast_background_failed
                                        )
                                    ).show()
                            }
                        }

                        override fun onFailure(
                            call: Call<LoginResponse?>,
                            throwable: Throwable
                        ) {
                            loginText.visibility = View.VISIBLE
                            loadingBar.visibility = View.GONE
                            Log.e("LoginActivity", "Error: ${throwable.message}")
                            throwable.printStackTrace()
                            Snackbar.make(
                                binding.root,
                                if (getString(R.string.lang) == "in")
                                    "Terjadi kesalahan. Silakan coba lagi!"
                                else
                                    "Something went wrong. Please try again!",
                                Snackbar.LENGTH_SHORT
                            ).also {
                                with(it) {
                                    setAction("Retry") {
                                        checkLogin(username, password)
                                    }
                                }
                            }.show()
                        }
                    })
            } catch (exception: Exception) {
                loginText.visibility = View.VISIBLE
                loadingBar.visibility = View.GONE
                Snackbar.make(
                    binding.root,
                    if (getString(R.string.lang) == "in")
                        "Terjadi kesalahan. Silakan coba lagi!"
                    else
                        "Something went wrong. Please try again!",
                    Snackbar.LENGTH_SHORT
                ).also {
                    with(it) {
                        setAction(if (getString(R.string.lang) == "in") "Ulangi" else "Retry") {
                            checkLogin(username, password)
                        }
                    }
                }.show()
                Log.e("LoginActivity", "Error: ${exception.message}")
                exception.printStackTrace()
            }
        }
    }

    private fun checkNewUpdate() {
        InitAPI.getEndpoint.checkUpdate(
            ChannelPickerBottomSheet.loadEffectiveChannel(this),
            BuildConfig.VERSION_NAME
        )
            .enqueue(object : Callback<UpdateCheckResponse> {
                override fun onResponse(
                    call: Call<UpdateCheckResponse>,
                    response: Response<UpdateCheckResponse>
                ) {
                    val body = response.body() ?: return
                    if (body.code == 1) {
                        val url = body.downloadUrl?.takeIf { it.isNotBlank() } ?: return
                        newAppVersion = body.versionName
                        val dialog = UpdateAvailableDialog(this@LoginActivity, url)
                            .also { d ->
                                d.setOnDownloadListener(object :
                                    UpdateAvailableDialog.OnDownloadListener {
                                    override fun onDownload(url: String) {
                                        startDownload(url)
                                    }
                                })
                            }
                        if (dialog.window != null && !isFinishing && !isDestroyed)
                            dialog.show()
                    }
                }

                override fun onFailure(
                    call: Call<UpdateCheckResponse>,
                    t: Throwable
                ) {
                    Log.e("ERROR Update Check", "checkUpdate failed: ${t.message}")
                }
            })
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
                    this@LoginActivity,
                    R.color.custom_toast_font_failed
                )
            )
            .setBackgroundColor(
                ContextCompat.getColor(
                    this@LoginActivity,
                    R.color.custom_toast_background_failed
                )
            ).show()
    }

    private fun startDownload(url: String) {
        val fileName = url.substringAfterLast('/').let {
            if (it.endsWith(".apk")) it else "FixMe_update_${BuildConfig.VERSION_NAME}.apk"
        }
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
                "FixMe Updates/$fileName"
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
                    this@LoginActivity,
                    R.color.custom_toast_font_blue
                )
            )
            .setBackgroundColor(
                ContextCompat.getColor(
                    this@LoginActivity,
                    R.color.custom_toast_background_soft_blue
                )
            ).show()
        showDownloadProgressNotification(downloadId.toInt(), 0)
    }

    private fun installApk(uri: Uri) {
        try {
            val downloadedFile = File(uri.path!!)
            pendingDeleteApkFile = downloadedFile
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    FileProvider.getUriForFile(
                        this@LoginActivity,
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
                pendingDeleteApkFile?.delete()
                pendingDeleteApkFile = null
                CustomToast.getInstance(applicationContext)
                    .setMessage(
                        if (getString(R.string.lang) == "in")
                            "Tidak ada aktivitas yang ditemukan untuk menangani instalasi APK."
                        else
                            "No activity found to handle APK installation."
                    )
                    .setFontColor(
                        ContextCompat.getColor(
                            this@LoginActivity,
                            R.color.custom_toast_font_failed
                        )
                    )
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            this@LoginActivity,
                            R.color.custom_toast_background_failed
                        )
                    ).show()
                ex.printStackTrace()
            }
        } catch (e: Exception) {
            pendingDeleteApkFile = null
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
                        this@LoginActivity,
                        R.color.custom_toast_font_failed
                    )
                )
                .setBackgroundColor(
                    ContextCompat.getColor(
                        this@LoginActivity,
                        R.color.custom_toast_background_failed
                    )
                ).show()
        }
    }

    private fun showDownloadProgressNotification(notificationId: Int, progress: Int) {
        val builder = NotificationCompat.Builder(this, "FixMe Download Channel")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Erela_FixMe_prerelease_v${newAppVersion}")
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
                    this@LoginActivity,
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