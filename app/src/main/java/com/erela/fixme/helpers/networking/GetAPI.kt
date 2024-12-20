package com.erela.fixme.helpers.networking

import com.erela.fixme.objects.CategoryListResponse
import com.erela.fixme.objects.CreationResponse
import com.erela.fixme.objects.DepartmentListResponse
import com.erela.fixme.objects.GenericSimpleResponse
import com.erela.fixme.objects.InboxResponse
import com.erela.fixme.objects.LoginResponse
import com.erela.fixme.objects.MaterialListResponse
import com.erela.fixme.objects.StarconnectUserResponse
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.SubmissionListResponse
import com.erela.fixme.objects.SupervisorListResponse
import com.erela.fixme.objects.TechnicianListResponse
import com.erela.fixme.objects.UserDetailResponse
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
    ): Call<GenericSimpleResponse>

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
    ): Call<CreationResponse>

    @Multipart
    @POST("pengajuansave")
    fun submitSubmissionNoAttachment(
        @PartMap submissionData: MutableMap<String, RequestBody>
    ): Call<CreationResponse>

    @Multipart
    @POST("updatepengajuan")
    fun updateSubmission(
        @PartMap submissionData: MutableMap<String, RequestBody>,
        @Part foto: List<MultipartBody.Part?>
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("updatepengajuan")
    fun updateSubmissionNoAttachment(
        @PartMap submissionData: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("setspv")
    fun getSupervisorList(
        @Field("id") id: Int
    ): Call<List<SupervisorListResponse>>

    @FormUrlEncoded
    @POST("setteknisi")
    fun getTechnicianList(
        @Field("id") id: Int
    ): Call<List<TechnicianListResponse>>

    @Multipart
    @POST("statusapprove")
    fun approveSubmission(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("statusreject")
    fun rejectSubmission(
        @Field("id_user") idUser: Int,
        @Field("id_gaprojects") idGaProjects: Int,
        @Field("keterangan") description: String
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("setupteknisi")
    fun deployTechnicians(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("saveprogress")
    fun createProgress(
        @PartMap data: MutableMap<String, RequestBody>,
        @Part foto: List<MultipartBody.Part?>
    ): Call<CreationResponse>

    @Multipart
    @POST("saveprogress")
    fun createProgressNoAttachment(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<CreationResponse>

    @FormUrlEncoded
    @POST("delprogress")
    fun deleteProgress(
        @Field("id") idProgress: Int,
        @Field("id_user") idUser: Int
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("updateprogress")
    fun editProgress(
        @PartMap data: MutableMap<String, RequestBody>,
        @Part foto: List<MultipartBody.Part?>
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("updateprogress")
    fun editProgressNoAttachment(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("progressdone")
    fun markProgressDone(
        @Field("id") id: Int,
        @Field("id_user") idUser: Int
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("progressdonetrial")
    fun markAsReadyForTrial(
        @Field("id") idGaProjects: Int,
        @Field("id_user") idUser: Int
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("stsontrial")
    fun startTrial(
        @Field("id") idGaProjects: Int,
        @Field("id_user") idUser: Int
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("savenotetrial")
    fun reportTrial(
        @Field("id_user") idUser: Int,
        @Field("id_gaprojects") idGaProjects: Int,
        @Field("keterangan") description: String,
        @Field("status") status: Int
    ): Call<CreationResponse>

    @FormUrlEncoded
    @POST("stsfinish")
    fun markIssueDone(
        @Field("id") idGaProjects: Int,
        @Field("id_user") idUser: Int
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("statuscancel")
    fun cancelSubmission(
        @Field("id_user") idUser: Int,
        @Field("id_gaprojects") idGaProjects: Int,
        @Field("keterangan") description: String
    ): Call<GenericSimpleResponse>
}