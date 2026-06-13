package com.erela.fixme.bottom_sheets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.BuildConfig
import com.erela.fixme.R
import com.erela.fixme.databinding.BottomSheetChannelPickerBinding
import com.erela.fixme.databinding.ItemChannelOptionBinding
import com.erela.fixme.dialogs.ConfirmationDialog
import com.google.android.material.bottomsheet.BottomSheetDialog

class ChannelPickerBottomSheet(context: Context) : BottomSheetDialog(context) {

    companion object {
        val CHANNEL_LADDER = listOf("release", "beta_prerelease", "dev", "canary")

        fun channelLevel(channel: String): Int =
            CHANNEL_LADDER.indexOf(channel).takeIf { it >= 0 } ?: 0
    }

    var effectiveChannel: String = BuildConfig.VERSION_CHANNEL
    var onChannelSelected: ((String) -> Unit)? = null

    private val binding: BottomSheetChannelPickerBinding by lazy {
        BottomSheetChannelPickerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(this@ChannelPickerBottomSheet.binding.root)

        this@ChannelPickerBottomSheet.binding.apply {
            channelList.layoutManager = LinearLayoutManager(context)
            channelList.adapter = ChannelAdapter()
        }
    }

    private inner class ChannelAdapter : RecyclerView.Adapter<ChannelAdapter.VH>() {
        inner class VH(val binding: ItemChannelOptionBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(ItemChannelOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount(): Int = CHANNEL_LADDER.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            with(holder.binding) {
                val channel = CHANNEL_LADDER[position]
                val bakedLevel = channelLevel(BuildConfig.VERSION_CHANNEL)
                val isLocked = channelLevel(channel) < bakedLevel
                val isCurrent = channel == effectiveChannel

                channelName.text = channelDisplayName(channel)
                channelDescription.text = channelDescription(channel)
                channelActiveTag.visibility = if (isCurrent) View.VISIBLE else View.GONE
                channelLockedHint.visibility = if (isLocked) View.VISIBLE else View.GONE

                when {
                    isLocked -> {
                        root.alpha = 0.4f
                        root.isClickable = false
                        channelEndIcon.setImageDrawable(
                            ContextCompat.getDrawable(root.context, R.drawable.baseline_lock_24)
                        )
                        channelEndIcon.alpha = 0.4f
                        channelEndIcon.imageTintList = null
                    }

                    isCurrent -> {
                        root.alpha = 1.0f
                        root.isClickable = false
                        channelEndIcon.setImageDrawable(
                            ContextCompat.getDrawable(
                                root.context,
                                R.drawable.baseline_check_circle_outline_24
                            )
                        )
                        channelEndIcon.alpha = 1.0f
                        channelEndIcon.imageTintList =
                            ContextCompat.getColorStateList(
                                root.context,
                                R.color.custom_toast_font_success
                            )
                    }

                    else -> {
                        root.alpha = 1.0f
                        root.isClickable = true
                        channelEndIcon.setImageDrawable(
                            ContextCompat.getDrawable(
                                root.context,
                                R.drawable.baseline_arrow_forward_ios_24
                            )
                        )
                        channelEndIcon.alpha = 0.4f
                        channelEndIcon.imageTintList = null
                        root.setOnClickListener { showConfirmDialog(channel) }
                    }
                }
            }
        }

        private fun showConfirmDialog(targetChannel: String) {
            ConfirmationDialog(
                context,
                context.getString(
                    R.string.channel_confirm_body,
                    channelDisplayName(effectiveChannel),
                    channelDisplayName(targetChannel)
                ),
                context.getString(R.string.channel_confirm_yes)
            ).apply {
                setConfirmationDialogListener(object :
                    ConfirmationDialog.ConfirmationDialogListener {
                    override fun onConfirm() {
                        onChannelSelected?.invoke(targetChannel)
                        dismiss()
                        this@ChannelPickerBottomSheet.dismiss()
                    }
                })
            }.show()
        }

        private fun channelDisplayName(channel: String): String = when (channel) {
            "release" -> context.getString(R.string.channel_name_release)
            "beta_prerelease" -> context.getString(R.string.channel_name_beta)
            "dev" -> context.getString(R.string.channel_name_dev)
            "canary" -> context.getString(R.string.channel_name_canary)
            else -> channel
        }

        private fun channelDescription(channel: String): String = when (channel) {
            "release" -> context.getString(R.string.channel_desc_release)
            "beta_prerelease" -> context.getString(R.string.channel_desc_beta)
            "dev" -> context.getString(R.string.channel_desc_dev)
            "canary" -> context.getString(R.string.channel_desc_canary)
            else -> ""
        }
    }
}
