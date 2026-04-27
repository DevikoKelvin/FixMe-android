package com.erela.fixme.bottom_sheets

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.erela.fixme.databinding.BsAcCheckInBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class AcCheckInBottomSheet(
    context: Context,
    private val itemId: Int,
    private val acCode: String?
) : BottomSheetDialog(context) {
    private val binding: BsAcCheckInBinding by lazy {
        BsAcCheckInBinding.inflate(layoutInflater)
    }

    private var onCheckInListener: OnCheckInListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    private fun init() {
        binding.apply {
            tvAcCode.text = acCode ?: "-"
            tvItemId.text = "$itemId"

            btnCheckIn.setOnClickListener {
                dismiss()
                onCheckInListener?.onCheckInRequested(itemId)
            }
        }
    }

    fun setOnCheckInListener(listener: OnCheckInListener) {
        onCheckInListener = listener
    }

    interface OnCheckInListener {
        fun onCheckInRequested(itemId: Int)
    }
}
