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
import com.erela.fixme.objects.SupervisorTechnicianListResponse
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
    @POST("getUserFromStarconnect")
    fun getUserFromStarConnect(
        @Field("id_user") idUser: Int
    ): Call<StarconnectUserResponse>

    @FormUrlEncoded
    @POST("checkLogin")
    fun login(
        @Field("usern") username: String,
        @Field("passw") password: String
    ): Call<LoginResponse>

    @FormUrlEncoded
    @POST("changePassword")
    fun changePassword(
        @Field("id_user") id: Int,
        @Field("passw") password: String
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("showInbox")
    fun showInbox(
        @Field("id_user") id: Int
    ): Call<List<InboxResponse>>

    @GET("getDeptartmentList")
    fun getDepartmentList(): Call<List<DepartmentListResponse>>

    @GET("getCategoryList")
    fun getCategoryList(): Call<List<CategoryListResponse>>

    @GET("getMaterialList")
    fun getMaterialList(): Call<List<MaterialListResponse>>

    @FormUrlEncoded
    @POST("reportList")
    fun getSubmissionList(
        @Field("id_user") id: Int,
        @Field("dept") department: String
    ): Call<List<SubmissionListResponse>>

    @FormUrlEncoded
    @POST("reportDetail")
    fun getSubmissionDetail(
        @Field("id") submissionID: String
    ): Call<List<SubmissionDetailResponse>>

    @Multipart
    @POST("submitReport")
    fun submitSubmission(
        @PartMap submissionData: MutableMap<String, RequestBody>,
        @Part foto: List<MultipartBody.Part?>
    ): Call<CreationResponse>

    @Multipart
    @POST("submitReport")
    fun submitSubmissionNoAttachment(
        @PartMap submissionData: MutableMap<String, RequestBody>
    ): Call<CreationResponse>

    @Multipart
    @POST("reportUpdate")
    fun updateSubmission(
        @PartMap submissionData: MutableMap<String, RequestBody>,
        @Part foto: List<MultipartBody.Part?>
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("reportUpdate")
    fun updateSubmissionNoAttachment(
        @PartMap submissionData: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("getSpv")
    fun getSupervisorList(
        @Field("id") id: Int
    ): Call<List<SupervisorTechnicianListResponse>>

    @FormUrlEncoded
    @POST("getTechnician")
    fun getTechnicianList(
        @Field("id") id: Int
    ): Call<List<SupervisorTechnicianListResponse>>

    @Multipart
    @POST("statusApproveTargetManager")
    fun approveTargetManagerSubmission(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("statusApproveReportManager")
    fun approveReportManagerSubmission(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("statusReject")
    fun rejectSubmission(
        @Field("id_user") idUser: Int,
        @Field("id_gaprojects") idGaProjects: Int,
        @Field("keterangan") description: String
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("setupTechnician")
    fun deployTechnicians(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("submitProgress")
    fun createProgress(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<CreationResponse>

    @FormUrlEncoded
    @POST("deleteProgress")
    fun deleteProgress(
        @Field("id") idProgress: Int,
        @Field("id_user") idUser: Int
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("updateProgress")
    fun editProgress(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("progressDone")
    fun markProgressDone(
        @Field("id") id: Int,
        @Field("id_user") idUser: Int
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("markReadyForTrial")
    fun markAsReadyForTrial(
        @Field("id") idGaProjects: Int,
        @Field("id_user") idUser: Int
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("startTrial")
    fun startTrial(
        @Field("id") idGaProjects: Int,
        @Field("id_user") idUser: Int
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("reportTrial")
    fun reportTrial(
        @Field("id_user") idUser: Int,
        @Field("id_gaprojects") idGaProjects: Int,
        @Field("keterangan") description: String,
        @Field("status") status: Int
    ): Call<CreationResponse>

    @FormUrlEncoded
    @POST("markIssueAsDone")
    fun markIssueDone(
        @Field("id") idGaProjects: Int,
        @Field("id_user") idUser: Int
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("cancelReport")
    fun cancelSubmission(
        @Field("id_user") idUser: Int,
        @Field("id_gaprojects") idGaProjects: Int,
        @Field("keterangan") description: String
    ): Call<GenericSimpleResponse>
}