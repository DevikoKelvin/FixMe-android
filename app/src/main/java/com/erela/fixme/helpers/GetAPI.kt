package com.erela.fixme.helpers

import com.erela.fixme.objects.ChangePasswordResponse
import com.erela.fixme.objects.LoginResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.Call

interface GetAPI {
    @FormUrlEncoded
    @POST("checklogin")
    fun login(
        @Field("usern") username: String,
        @Field("passw") password: String
    ): Call<LoginResponse>

    @FormUrlEncoded
    @POST("ubahpass")
    fun changePassword(
        @Field("id_user") id: Int,
        @Field("passw") password: String
    ): Call<ChangePasswordResponse>
}