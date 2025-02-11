package com.erela.fixme.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.R
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityChangePasswordBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.GenericSimpleResponse
import com.erela.fixme.objects.UserData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordActivity : AppCompatActivity() {
    private val binding: ActivityChangePasswordBinding by lazy {
        ActivityChangePasswordBinding.inflate(layoutInflater)
    }
    private lateinit var userData: UserData
    private var passValid = false
    private var passMatch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userData = UserDataHelper(this).getUserData()

        init()
    }

    private fun init() {
        binding.apply {
            backButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            passwordField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    if (s != null)
                        if (s.length < 4) {
                            passwordFieldLayout.error =
                                getString(R.string.password_minimum_char_error)
                            passValid = false
                        } else {
                            passwordFieldLayout.error = null
                            passValid = true
                        }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
            confirmPasswordField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    if (s != null)
                        if (s.length > 4) {
                            if (s.toString() != passwordField.text.toString()) {
                                confirmPasswordFieldLayout.error =
                                    getString(R.string.password_not_match_error)
                                passMatch = false
                            } else {
                                confirmPasswordFieldLayout.error = null
                                passMatch = true
                            }
                        } else {
                            confirmPasswordFieldLayout.error =
                                getString(R.string.password_minimum_char_error)
                            passMatch = false
                        }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            saveSettingsButton.setOnClickListener {
                if (!passValid || !passMatch) {
                    CustomToast.getInstance(this@ChangePasswordActivity)
                        .setBackgroundColor(getColor(R.color.custom_toast_background_failed))
                        .setFontColor(getColor(R.color.custom_toast_font_failed))
                        .setMessage(
                            "Please check your password or confirmation password and make sure all requirement meets"
                        ).show()
                } else {
                    try {
                        InitAPI.getAPI.changePassword(
                            userData.id, confirmPasswordField.text.toString()
                        )
                            .enqueue(object : Callback<GenericSimpleResponse> {
                                override fun onResponse(
                                    call: Call<GenericSimpleResponse?>,
                                    response: Response<GenericSimpleResponse?>
                                ) {
                                    if (response.body() != null) {
                                        if (response.body()?.code == 1) {
                                            CustomToast.getInstance(this@ChangePasswordActivity)
                                                .setBackgroundColor(
                                                    getColor(
                                                        R.color.custom_toast_background_success
                                                    )
                                                )
                                                .setFontColor(
                                                    getColor(R.color.custom_toast_font_success)
                                                )
                                                .setMessage("Setting saved!")
                                                .show()
                                            finish()
                                        } else
                                            CustomToast.getInstance(this@ChangePasswordActivity)
                                                .setBackgroundColor(
                                                    getColor(R.color.custom_toast_background_failed)
                                                )
                                                .setFontColor(
                                                    getColor(R.color.custom_toast_font_failed)
                                                )
                                                .setMessage(
                                                    "Please check your password or confirmation password and make sure all requirement meets"
                                                ).show()
                                    }
                                }

                                override fun onFailure(
                                    call: Call<GenericSimpleResponse?>,
                                    throwable: Throwable
                                ) {
                                    throwable.printStackTrace()
                                    CustomToast.getInstance(this@ChangePasswordActivity)
                                        .setBackgroundColor(
                                            getColor(R.color.custom_toast_background_failed)
                                        )
                                        .setFontColor(getColor(R.color.custom_toast_font_failed))
                                        .setMessage(
                                            "Something went wrong, please try again later"
                                        ).show()
                                }
                            })
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                }
            }
        }
    }
}