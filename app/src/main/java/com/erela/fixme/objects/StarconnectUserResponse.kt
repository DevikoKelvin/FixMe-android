package com.erela.fixme.objects

import com.google.gson.annotations.SerializedName

data class StarconnectUserResponse(
    @field:SerializedName("MEMKID")
    val mEMKID: String? = null,
    @field:SerializedName("MEMINIT")
    val mEMINIT: String? = null,
    @field:SerializedName("MEMORGLVL")
    val mEMORGLVL: String? = null,
    @field:SerializedName("MEMORG")
    val mEMORG: String? = null,
    @field:SerializedName("MEMNAME")
    val mEMNAME: String? = null
)
