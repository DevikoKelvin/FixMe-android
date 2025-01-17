package com.erela.fixme.bottom_sheets

import android.content.Context
import android.os.Bundle
import com.erela.fixme.databinding.BsMaterialQuantityBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale

class MaterialQuantityBottomSheet(context: Context): BottomSheetDialog(context) {
    private val binding: BsMaterialQuantityBinding by lazy {
        BsMaterialQuantityBinding.inflate(layoutInflater)
    }
    private lateinit var onQuantityConfirmListener: OnQuantityConfirmListener
    private var counter: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        init()
    }

    private fun init() {
        binding.apply {
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
    }
}