package com.erela.fixme.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
                if (usernameField.text.isNullOrEmpty() || passwordField.text.isNullOrEmpty()) {
                    Snackbar.make(
                        binding.root,
                        "Please fill in all fields.",
                        Snackbar.LENGTH_SHORT
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

            try {
                InitAPI.getAPI.login(username, password)
                    .enqueue(object : Callback<LoginResponse> {
                        override fun onResponse(
                            call: Call<LoginResponse?>,
                            response: Response<LoginResponse?>
                        ) {
                            when (response.body()?.code) {
                                0 -> {
                                    showSnackBar(
                                        "Wrong password. Please try again!",
                                        true,
                                        username,
                                        password
                                    )
                                    passwordFieldLayout.error = "Wrong password"
                                }

                                1 -> {
                                    showSnackBar("Login successful!", true, username, password)
                                    UserDataHelper(this@LoginActivity)
                                        .setUserData(
                                            response.body()?.idUser!!.toInt(),
                                            UsernameFormatHelper.getRealUsername(username),
                                            response.body()?.hakAkses!!.toInt()
                                        )
                                    Handler(mainLooper).postDelayed({
                                        showSnackBar(
                                            "Welcome, ${
                                                UsernameFormatHelper.getRealUsername(
                                                    username
                                                )
                                            }!", true, username, password
                                        )
                                        Handler(mainLooper).postDelayed({
                                            startActivity(
                                                Intent(
                                                    this@LoginActivity,
                                                    MainActivity::class.java
                                                )
                                            ).also {
                                                finish()
                                            }
                                        }, 2000)
                                    }, 2000)
                                }

                                2 -> {
                                    showSnackBar(
                                        "User not found. Please contact the IT department.",
                                        true,
                                        username,
                                        password
                                    )
                                }
                            }
                        }

                        override fun onFailure(
                            call: Call<LoginResponse?>,
                            throwable: Throwable
                        ) {
                            throwable.printStackTrace()
                            showSnackBar(
                                "Something went wrong. Please try again!",
                                false,
                                username,
                                password
                            )
                        }
                    })
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
        }
    }

    private fun showSnackBar(
        message: String,
        isValid: Boolean,
        username: String,
        password: String
    ) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_SHORT
        ).also {
            if (!isValid) {
                it.setAction("Retry", View.OnClickListener {
                    checkLogin(username, password)
                })
            }
        }.show()
    }
}