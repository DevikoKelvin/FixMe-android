package com.erela.fixme.activities

import com.erela.fixme.R
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.erela.fixme.activities.SubmissionListActivity
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityLoginBinding
import com.erela.fixme.helpers.InitAPI
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.UsernameFormatHelper
import com.erela.fixme.objects.LoginResponse
import com.erela.fixme.objects.UserData
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var userData: UserData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userData = UserDataHelper(this@LoginActivity).getUserData()

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
                        .setMessage("Please fill in all fields.")
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
                        usernameFieldLayout.error = "Username cannot be empty"
                    } else {
                        usernameFieldLayout.error = null
                    }
                    if (passwordField.text.isNullOrEmpty()) {
                        passwordFieldLayout.error = "Password cannot be empty"
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
                                    when (response.body()?.code) {
                                        0 -> {
                                            CustomToast.getInstance(applicationContext)
                                                .setMessage("Wrong password. Please try again!")
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
                                            passwordFieldLayout.error = "Wrong password"
                                        }

                                        1 -> {
                                            CustomToast.getInstance(applicationContext)
                                                .setMessage("Login Successful!")
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
                                                    response.body()?.idUser!!.toInt(),
                                                    username,
                                                    response.body()?.nama.toString(),
                                                    response.body()?.hakAkses!!.toInt(),
                                                    response.body()?.idDept!!.toInt()
                                                )
                                            Handler(mainLooper).postDelayed({
                                                CustomToast.getInstance(applicationContext)
                                                    .setMessage(
                                                        "Welcome, ${
                                                            UsernameFormatHelper.getRealUsername(
                                                                username
                                                            )
                                                        }!"
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
                                        .setMessage("Something went wrong, please try again.")
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
                                    .setMessage("Something went wrong, please try again.")
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
                            Log.e("ERROR", throwable.toString())
                            throwable.printStackTrace()
                            Snackbar.make(
                                binding.root,
                                "Something went wrong. Please try again!",
                                Snackbar.LENGTH_SHORT
                            ).also {
                                with(it) {
                                    setAction("Retry", View.OnClickListener {
                                        checkLogin(username, password)
                                    })
                                }
                            }.show()
                        }
                    })
            } catch (exception: Exception) {
                loginText.visibility = View.VISIBLE
                loadingBar.visibility = View.GONE
                Log.e("ERROR", exception.toString())
                Snackbar.make(
                    binding.root,
                    "Something went wrong. Please try again!",
                    Snackbar.LENGTH_SHORT
                ).also {
                    with(it) {
                        setAction("Retry", View.OnClickListener {
                            checkLogin(username, password)
                        })
                    }
                }.show()
                exception.printStackTrace()
            }
        }
    }
}