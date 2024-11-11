package com.erela.fixme.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import com.erela.fixme.R
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityChangePasswordBinding
import com.erela.fixme.helpers.InitAPI
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.ChangePasswordResponse
import com.erela.fixme.objects.UserData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var userData: UserData
    private var passValid = false
    private var passMatch = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
                    InitAPI.getAPI.changePassword(userData.id, confirmPasswordField.text.toString())
                        .enqueue(object : Callback<ChangePasswordResponse> {
                            override fun onResponse(
                                call: Call<ChangePasswordResponse?>,
                                response: Response<ChangePasswordResponse?>
                            ) {
                                if (response.body() != null) {
                                    if (response.body()?.code == 1) {
                                        CustomToast.getInstance(this@ChangePasswordActivity)
                                            .setBackgroundColor(
                                                getColor(R.color.custom_toast_background_success)
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
                                call: Call<ChangePasswordResponse?>,
                                throwable: Throwable
                            ) {
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
                }
            }
        }
    }
}