package com.erela.fixme.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import com.erela.fixme.R
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.DialogSignaturePadBinding
import java.io.File
import java.util.UUID

/**
 * Where the AC maintenance witness signs.
 *
 * The pad was inline on the check-out form, inside its ScrollView. Even with the view claiming the
 * gesture, a finger landing on it mid-scroll could leave a stray mark, and every stroke committed
 * straight to the signature that would be submitted. Here the drawing is a draft: Cancel throws it
 * away and only Save hands a file back, which is the same behaviour as the Compose app.
 *
 * Not cancelable by touching outside, matching ConfirmationDialog — an accidental dismissal
 * halfway through signing means asking the PIC to sign again.
 */
class SignaturePadDialog(context: Context) : Dialog(context) {
    private val binding: DialogSignaturePadBinding by lazy {
        DialogSignaturePadBinding.inflate(layoutInflater)
    }
    private var listener: OnSignatureSavedListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        setCancelable(false)

        init()
    }

    private fun init() {
        binding.apply {
            // Nothing to clear until something is drawn. Driven off the pad's own state rather than
            // set at each call site, so clear(), a finished stroke and the initial state can never
            // disagree about whether the button belongs on screen.
            clearButton.visibility = if (signaturePad.isEmpty) View.GONE else View.VISIBLE
            signaturePad.onEmptyStateChanged = { empty ->
                clearButton.visibility = if (empty) View.GONE else View.VISIBLE
            }

            clearButton.setOnClickListener { signaturePad.clear() }

            cancelButton.setOnClickListener {
                // Nothing is written anywhere on this path: the strokes live in the pad view and go
                // with the dialog. Whatever was saved before stays as it was.
                dismiss()
            }

            saveButton.setOnClickListener {
                if (signaturePad.isEmpty) {
                    showFailure(
                        "Please ask the room PIC to sign first",
                        "Mohon minta PIC ruangan untuk menandatangani terlebih dahulu."
                    )
                    return@setOnClickListener
                }
                val target = File(context.externalCacheDir, "AC_sign_${UUID.randomUUID()}.png")
                if (!signaturePad.saveAsPng(target)) {
                    showFailure(
                        "Could not save the signature. Please sign again.",
                        "Tanda tangan gagal disimpan. Mohon tanda tangani ulang."
                    )
                    return@setOnClickListener
                }

                listener?.onSignatureSaved(target)
                dismiss()
            }
        }
    }

    private fun showFailure(english: String, indonesian: String) {
        CustomToast.getInstance(context)
            .setMessage(if (context.getString(R.string.lang) == "en") english else indonesian)
            .setBackgroundColor(
                ResourcesCompat.getColor(
                    context.resources, R.color.custom_toast_background_failed, context.theme
                )
            )
            .setFontColor(
                ResourcesCompat.getColor(
                    context.resources, R.color.custom_toast_font_failed, context.theme
                )
            )
            .show()
    }

    fun setOnSignatureSavedListener(listener: OnSignatureSavedListener) {
        this.listener = listener
    }

    interface OnSignatureSavedListener {
        /** Called only on Save, with a PNG written to the app's external cache. */
        fun onSignatureSaved(file: File)
    }
}
