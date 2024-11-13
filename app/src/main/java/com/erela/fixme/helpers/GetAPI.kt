package com.erela.fixme.helpers

import com.erela.fixme.objects.ChangePasswordResponse
import com.erela.fixme.objects.DepartmentListResponse
import com.erela.fixme.objects.InboxResponse
import com.erela.fixme.objects.LoginResponse
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.SubmissionListResponse
import com.erela.fixme.objects.UserListResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface GetAPI {
    @GET("getusers")
    fun getUserList(): Call<List<UserListResponse>>

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

    @FormUrlEncoded
    @POST("showinbox")
    fun showInbox(
        @Field("id_user") id: Int
    ): Call<List<InboxResponse>>

    @GET("depttuj")
    fun getDepartmentList(): Call <List<DepartmentListResponse>>

    @FormUrlEncoded
    @POST("listpengajuan")
    fun getSubmissionList(
        @Field("id_user") id: Int,
        @Field("dept") department: String
    ): Call<List<SubmissionListResponse>>

    @FormUrlEncoded
    @POST("detailpengajuan")
    fun getSubmissionDetail(
        @Field("id") submissionID : String
    ): Call<List<SubmissionDetailResponse>>
}