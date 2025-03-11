package com.erela.fixme.bottom_sheets

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.erela.fixme.databinding.BsMaterialQuantityBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale

class MaterialQuantityBottomSheet(context: Context, private val materialName: String) :
    BottomSheetDialog(context) {
    private val binding: BsMaterialQuantityBinding by lazy {
        BsMaterialQuantityBinding.inflate(layoutInflater)
    }
    private lateinit var onQuantityConfirmListener: OnQuantityConfirmListener
    private var counter: Int = 1

    constructor(context: Context, materialName: String, counter: Int) : this(
        context,
        materialName
    ) {
        this.counter = counter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    override fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) {
        onQuantityConfirmListener.onBottomSheetDismissed(counter)
        super.setOnDismissListener(listener)
    }

    private fun init() {
        binding.apply {
            materialNameText.text = materialName
            counterText.text = String.format(Locale.getDefault(), "%d", counter)
            decreaseButton.setOnClickListener {
                if (counter == 1)
                    return@setOnClickListener
                counter--
                counterText.text = String.format(Locale.getDefault(), "%d", counter)
            }
            increaseButton.setOnClickListener {
                counter++
                counterText.text = String.format(Locale.getDefault(), "%d", counter)
            }
            confirmButton.setOnClickListener {
                onQuantityConfirmListener.onQuantityConfirm(counter)
                dismiss()
            }
        }
    }

    fun setOnQuantityConfirmListener(onQuantityConfirmListener: OnQuantityConfirmListener) {
        this.onQuantityConfirmListener = onQuantityConfirmListener
    }

    interface OnQuantityConfirmListener {
        fun onQuantityConfirm(quantity: Int)
        fun onBottomSheetDismissed(quantity: Int)
    }
}