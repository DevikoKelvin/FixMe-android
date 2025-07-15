package com.erela.fixme.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.R
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityLoginBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.LoginResponse
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private val binding: ActivityLoginBinding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()
    }

    private fun init() {
        binding.apply {
            if (UserDataHelper(this@LoginActivity).isUserDataExist()) {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java)).also {
                    finish()
                }
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
                    checkLogin(usernameField.text.toString(), passwordField.text.toString())
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
                InitAPI.getAPI.login(username, password)
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
                                    val name = result?.nama!!
                                    when (result.code) {
                                        0 -> {
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
                                                    result.idUser!!,
                                                    result.idStarConnect!!,
                                                    username,
                                                    name,
                                                    result.hakAkses!!,
                                                    result.idDept!!,
                                                    result.dept!!,
                                                    result.subDept!!
                                                )
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
                                    }
                                } else {
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
                exception.printStackTrace()
            }
        }
    }
}