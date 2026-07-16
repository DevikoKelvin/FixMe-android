package com.erela.fixme.activities

import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.erela.fixme.helpers.enableEdgeToEdgeOpaqueNav
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.erela.fixme.R
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityChangeEmailBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.GenericSimpleResponse
import com.erela.fixme.objects.UserData
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangeEmailActivity : AppCompatActivity() {
    private val binding: ActivityChangeEmailBinding by lazy {
        ActivityChangeEmailBinding.inflate(layoutInflater)
    }
    private val userDataHelper: UserDataHelper by lazy { UserDataHelper(this) }
    private val userData: UserData by lazy { userDataHelper.getUserData() }
    private var emailValid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdgeOpaqueNav()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
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

    private fun init() {
        binding.apply {
            currentEmailText.text = userData.email.ifBlank {
                if (getString(R.string.lang) == "in") "Belum diatur" else "Not set"
            }

            backButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            newEmailField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun afterTextChanged(s: Editable?) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s != null) {
                        if (!Patterns.EMAIL_ADDRESS.matcher(s).matches()) {
                            newEmailFieldLayout.error = getString(R.string.email_not_valid_error)
                            emailValid = false
                        } else {
                            newEmailFieldLayout.error = null
                            emailValid = true
                        }
                    }
                }
            })

            saveSettingsButton.setOnClickListener {
                val newEmail = newEmailField.text.toString().trim()
                val password = confirmPasswordField.text.toString()

                if (!emailValid || newEmail.isBlank()) {
                    showToast(
                        getString(R.string.email_not_valid_error),
                        success = false
                    )
                    return@setOnClickListener
                }

                if (password.isBlank()) {
                    showToast(
                        if (getString(R.string.lang) == "in") "Harap masukkan kata sandi"
                        else "Please enter your password",
                        success = false
                    )
                    return@setOnClickListener
                }

                try {
                    InitAPI.getEndpoint.changeEmail(userData.id, password, newEmail)
                        .enqueue(object : Callback<GenericSimpleResponse> {
                            override fun onResponse(
                                call: Call<GenericSimpleResponse?>,
                                response: Response<GenericSimpleResponse?>
                            ) {
                                val body = response.body() ?: return
                                if (body.code == 1) {
                                    userDataHelper.saveEmail(newEmail)
                                    showToast(
                                        if (getString(R.string.lang) == "in")
                                            "Email berhasil diubah!"
                                        else
                                            "Email successfully changed!",
                                        success = true
                                    )
                                    finish()
                                } else {
                                    val msg = when (body.code) {
                                        -1 -> if (getString(R.string.lang) == "in") "Harap lengkapi semua data" else "Please fill in all fields"
                                        -2 -> if (getString(R.string.lang) == "in") "Format email tidak valid" else "Invalid email format"
                                        -3 -> if (getString(R.string.lang) == "in") "User tidak ditemukan" else "User not found"
                                        -4 -> if (getString(R.string.lang) == "in") "Password salah" else "Wrong password"
                                        -5 -> if (getString(R.string.lang) == "in") "Email sudah digunakan akun lain" else "Email already used by another account"
                                        else -> body.message
                                            ?: if (getString(R.string.lang) == "in") "Terjadi kesalahan" else "Something went wrong"
                                    }
                                    showToast(msg, success = false)
                                }
                            }

                            override fun onFailure(
                                call: Call<GenericSimpleResponse?>,
                                throwable: Throwable
                            ) {
                                throwable.printStackTrace()
                                showToast(
                                    if (getString(R.string.lang) == "in")
                                        "Terjadi kesalahan, silakan coba lagi nanti"
                                    else
                                        "Something went wrong, please try again later",
                                    success = false
                                )
                            }
                        })
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }
        }
    }

    private fun showToast(message: String, success: Boolean) {
        val bg =
            if (success) R.color.custom_toast_background_success else R.color.custom_toast_background_failed
        val fg =
            if (success) R.color.custom_toast_font_success else R.color.custom_toast_font_failed
        CustomToast.getInstance(this)
            .setBackgroundColor(getColor(bg))
            .setFontColor(getColor(fg))
            .setMessage(message)
            .show()
    }
}
