package com.erela.fixme.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.graphics.drawable.toDrawable
import com.erela.fixme.databinding.DialogOptionListBinding
import com.erela.fixme.databinding.ListItemDialogOptionBinding

/** One row of an option dialog: what it says, and the quieter line under it when there is one. */
data class DialogOption(val label: String, val subtitle: String? = null)

/**
 * Pick one of a few things — a condition, a collector, a printer.
 *
 * THE HOUSE DIALOG, NOT MATERIAL'S `setItems`. [ConfirmationDialog] set the shape: white card,
 * divider, flat cancel in the failure colour. Material's list dialog brings its own type scale and
 * its own corner radius onto a screen that already has both.
 *
 * ROWS ARE ADDED AT RUNTIME. The lists are two to eight items and each caller's is a different
 * shape of thing; a RecyclerView and an adapter for eight rows that never scroll is machinery for
 * its own sake.
 *
 * PICKING A ROW IS THE CONFIRMATION, so the only button cancels. A second tap to agree with the
 * tap just made is how somebody chooses the wrong printer twice.
 */
class OptionListDialog(context: Context) : Dialog(context) {
    private val binding: DialogOptionListBinding by lazy {
        DialogOptionListBinding.inflate(layoutInflater)
    }

    private lateinit var listener: OptionListDialogListener
    private lateinit var title: String
    private lateinit var options: List<DialogOption>

    constructor(context: Context, title: String, options: List<DialogOption>) : this(context) {
        this.title = title
        this.options = options
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

            val inflater = LayoutInflater.from(context)

            options.forEachIndexed { index, option ->
                val row = ListItemDialogOptionBinding.inflate(inflater, optionContainer, false)

                row.optionLabel.text = option.label
                row.optionSubtitle.text = option.subtitle
                row.optionSubtitle.visibility =
                    if (option.subtitle.isNullOrBlank()) View.GONE else View.VISIBLE

                row.root.setOnClickListener {
                    listener.onPick(index)
                    dismiss()
                }

                optionContainer.addView(row.root)
            }

            cancelButton.setOnClickListener { dismiss() }
        }
    }

    fun setOptionListDialogListener(listener: OptionListDialogListener) {
        this.listener = listener
    }

    fun interface OptionListDialogListener {
        fun onPick(index: Int)
    }
}
