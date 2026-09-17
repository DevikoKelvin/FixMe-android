package com.erela.fixme.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.addTextChangedListener
import com.erela.fixme.databinding.DialogInputBinding

/**
 * Ask for one line of text — the reprint reason, a condition note.
 *
 * THE HOUSE DIALOG, NOT MATERIAL'S. [ConfirmationDialog] set the shape for this app long before
 * the laundry screens existed: a white card, a divider, and two flat buttons with the cancel in
 * the failure colour. An `AlertDialog.Builder` on top of that looks like a different application,
 * which is precisely what the reprint prompt looked like.
 *
 * THE CONFIRM BUTTON IS DEAD WHILE THE FIELD IS EMPTY. Every caller so far needs the text — a
 * reprint without a reason is refused by the server [3e] — so an empty submit could only ever
 * produce a refusal, and a button that explains itself beats one that does nothing.
 */
class InputDialog(context: Context) : Dialog(context) {
    private val binding: DialogInputBinding by lazy {
        DialogInputBinding.inflate(layoutInflater)
    }

    private lateinit var listener: InputDialogListener
    private lateinit var title: String
    private lateinit var hint: String
    private lateinit var confirmationText: String
    private var message: String? = null

    constructor(
        context: Context,
        title: String,
        hint: String,
        confirmationText: String,
        message: String? = null
    ) : this(context) {
        this.title = title
        this.hint = hint
        this.confirmationText = confirmationText
        this.message = message
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        setCancelable(false)

        init()
    }

    private fun init() {
        binding.apply {
            titleText.text = title
            inputField.hint = hint
            confirmText.text = confirmationText

            messageText.text = message
            messageText.visibility = if (message.isNullOrBlank()) View.GONE else View.VISIBLE

            setConfirmEnabled(false)

            inputText.addTextChangedListener(
                onTextChanged = { text, _, _, _ ->
                    setConfirmEnabled(!text.isNullOrBlank())
                }
            )

            cancelButton.setOnClickListener { dismiss() }

            confirmButton.setOnClickListener {
                val value = inputText.text?.toString()?.trim().orEmpty()

                if (value.isEmpty()) {
                    return@setOnClickListener
                }

                listener.onSubmit(value)
                dismiss()
            }
        }
    }

    private fun setConfirmEnabled(on: Boolean) {
        binding.confirmButton.apply {
            isClickable = on
            isFocusable = on
            alpha = if (on) 1f else 0.4f
        }
    }

    fun setInputDialogListener(listener: InputDialogListener) {
        this.listener = listener
    }

    fun interface InputDialogListener {
        fun onSubmit(value: String)
    }
}
