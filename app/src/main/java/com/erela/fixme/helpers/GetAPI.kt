package com.erela.fixme.helpers

import com.erela.fixme.objects.CategoryListResponse
import com.erela.fixme.objects.ChangePasswordResponse
import com.erela.fixme.objects.DepartmentListResponse
import com.erela.fixme.objects.InboxResponse
import com.erela.fixme.objects.LoginResponse
import com.erela.fixme.objects.MaterialListResponse
import com.erela.fixme.objects.StarconnectUserResponse
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.SubmissionListResponse
import com.erela.fixme.objects.SubmitSubmissionResponse
import com.erela.fixme.objects.UpdateStatusResponse
import com.erela.fixme.objects.UpdateSubmissionResponse
import com.erela.fixme.objects.UserDetailResponse
import com.erela.fixme.objects.UserListResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.PartMap

interface GetAPI {
    @GET("getusers")
    fun getUserList(): Call<List<UserListResponse>>

    @FormUrlEncoded
    @POST("getuserdetail")
    fun getUserDetail(
        @Field("id_user") idUser: Int
    ): Call<UserDetailResponse>

    @FormUrlEncoded
    @POST("getuserfromstarconnect")
    fun getUserFromStarConnect(
        @Field("id_user") idUser: Int
    ): Call<StarconnectUserResponse>

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
    fun getDepartmentList(): Call<List<DepartmentListResponse>>

    @GET("getkategorilist")
    fun getCategoryList(): Call<List<CategoryListResponse>>

    @GET("getmateriallist")
    fun getMaterialList(): Call<List<MaterialListResponse>>

    @FormUrlEncoded
    @POST("listpengajuan")
    fun getSubmissionList(
        @Field("id_user") id: Int,
        @Field("dept") department: String
    ): Call<List<SubmissionListResponse>>

    @FormUrlEncoded
    @POST("detailpengajuan")
    fun getSubmissionDetail(
        @Field("id") submissionID: String
    ): Call<List<SubmissionDetailResponse>>

    @Multipart
    @POST("pengajuansave")
    fun submitSubmission(
        @PartMap submissionData: MutableMap<String, RequestBody>,
        @Part foto: List<MultipartBody.Part?>
    ): Call<SubmitSubmissionResponse>

    @Multipart
    @POST("pengajuansave")
    fun submitSubmissionNoAttachment(
        @PartMap submissionData: MutableMap<String, RequestBody>
    ): Call<SubmitSubmissionResponse>

    @FormUrlEncoded
    @POST("statusupdate")
    fun updateSubmissionStatus(
        @Field("id_user") userId: Int,
        @Field("id") detailSubmissionId: Int,
        @Field("sts") status: Int,
        @Field("keterangan") description: String,
        @Field("user_end") targetUserId: Int,
        @Field("tgl_pengerjaan") workingDate: String,
        @Field("waktu_pengerjaan") workingTime: String
    ): Call<UpdateStatusResponse>

    @Multipart
    @POST("updatepengajuan")
    fun updateSubmission(
        @PartMap submissionData: MutableMap<String, RequestBody>,
        @Part foto: List<MultipartBody.Part?>
    ): Call<UpdateSubmissionResponse>

    @Multipart
    @POST("updatepengajuan")
    fun updateSubmissionNoAttachment(
        @PartMap submissionData: MutableMap<String, RequestBody>
    ): Call<UpdateSubmissionResponse>
}