package com.erela.fixme.bottom_sheets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import com.erela.fixme.R
import com.erela.fixme.databinding.BsUserInfoBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.UserData
import com.google.android.material.bottomsheet.BottomSheetDialog

class UserInfoBottomSheet(context: Context) : BottomSheetDialog(context) {
    private val binding: BsUserInfoBinding by lazy {
        BsUserInfoBinding.inflate(layoutInflater)
    }
    private val userData: UserData by lazy {
        UserDataHelper(context).getUserData()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setCancelable(true)

        init()
    }

    @SuppressLint("SetTextI18n")
    private fun init() {
        binding.apply {
            nameText.text = userData.name
            starConnectIdText.text = userData.idStarConnect.toString()
            fixMeUsernameText.text = userData.username
            privilegeText.text = when (userData.privilege) {
                0 -> if (context.getString(R.string.lang) == "in") "Pemilik" else "Owner"
                1 -> if (context.getString(R.string.lang) == "in") "Manajer/Asisten Manajer/Admin" else "Manager/Assistant Manager/Administrator"
                2 -> "Supervisor"
                3 -> if (context.getString(R.string.lang) == "in") "Teknisi" else "Technician"
                else -> if (context.getString(R.string.lang) == "in") "Staf/Pelapor" else "Staff/Reporter"
            }
            departmentText.text = userData.dept
            subDepartmentText.text = userData.subDept
        }
    }
}