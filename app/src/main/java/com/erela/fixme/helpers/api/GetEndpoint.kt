package com.erela.fixme.helpers.api

import com.erela.fixme.objects.CategoryListResponse
import com.erela.fixme.objects.CreationResponse
import com.erela.fixme.objects.DepartmentListResponse
import com.erela.fixme.objects.GenericSimpleResponse
import com.erela.fixme.objects.InboxResponse
import com.erela.fixme.objects.LoginResponse
import com.erela.fixme.objects.MaterialListResponse
import com.erela.fixme.objects.SubDepartmentListResponse
import com.erela.fixme.objects.SubmissionDetailResponse
import com.erela.fixme.objects.SubmissionListResponse
import com.erela.fixme.objects.SubmissionProgressResponse
import com.erela.fixme.objects.SubmissionTrialResponse
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

interface GetEndpoint {
    @FormUrlEncoded
    @POST("login")
    fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    @FormUrlEncoded
    @POST("updateFcmToken")
    fun updateFcmToken(
        @Field("user_id") userId: Int,
        @Field("token") token: String
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("changePassword")
    fun changePassword(
        @Field("user_id") userId: Int,
        @Field("old_password") oldPassword: String,
        @Field("new_password") newPassword: String,
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("checkInbox")
    fun showInbox(
        @Field("user_id") userId: Int
    ): Call<List<InboxResponse>>

    @GET("getTargetDepartment")
    fun getDepartmentList(): Call<List<DepartmentListResponse>>

    @FormUrlEncoded
    @POST("getTargetSubDept")
    fun getSubDepartmentList(
        @Field("dept_name") deptName: String
    ): Call<List<SubDepartmentListResponse>>

    @GET("getDepartmentList")
    fun getDeptList(): Call<List<DepartmentListResponse>>

    @GET("getCategoryList")
    fun getCategoryList(): Call<List<CategoryListResponse>>

    @FormUrlEncoded
    @POST("getMaterialList")
    fun getMaterialList(
        @Field("category_id") categoryId: Int
    ): Call<List<MaterialListResponse>>

    @FormUrlEncoded
    @POST("reportList")
    fun getSubmissionList(
        @Field("user_id") userId: Int,
        @Field("department") department: String
    ): Call<SubmissionListResponse>

    @FormUrlEncoded
    @POST("reportDetail")
    fun getSubmissionDetail(
        @Field("case_id") caseId: String,
        @Field("user_id") userId: Int
    ): Call<List<SubmissionDetailResponse>>

    @FormUrlEncoded
    @POST("getProgress")
    fun getSubmissionProgress(
        @Field("case_id") caseId: Int
    ): Call<SubmissionProgressResponse>

    @Multipart
    @POST("submitReport")
    fun submitSubmission(
        @PartMap submissionData: MutableMap<String, RequestBody>,
        @Part photo: ArrayList<MultipartBody.Part?>
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
        @Part photo: ArrayList<MultipartBody.Part?>
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("reportUpdate")
    fun updateSubmissionNoAttachment(
        @PartMap submissionData: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("cancelReport")
    fun cancelSubmission(
        @Field("user_id") userId: Int,
        @Field("case_id") caseId: Int,
        @Field("description") description: String
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("getSpv")
    fun getSupervisorList(
        @Field("case_id") caseId: Int,
        @Field("dept_id") deptId : Int
    ): Call<SupervisorTechnicianListResponse>

    @FormUrlEncoded
    @POST("getTechnician")
    fun getTechnicianList(
        @Field("case_id") caseId: Int
    ): Call<SupervisorTechnicianListResponse>

    @Multipart
    @POST("statusApproveReportManager")
    fun approveReportManagerSubmission(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("statusApproveTargetManager")
    fun approveTargetManagerSubmission(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("statusReject")
    fun rejectSubmission(
        @Field("user_id") idUser: Int,
        @Field("case_id") idGaProjects: Int,
        @Field("description") description: String
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("updateAssignedSpv")
    fun editApprovals(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("setupTechnician")
    fun deployTechnicians(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("updateAssignedTech")
    fun editTechnicians(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("submitProgress")
    fun createProgress(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<CreationResponse>

    @Multipart
    @POST("requestMaterialAdd")
    fun requestMaterialAdd(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("deleteProgress")
    fun deleteProgress(
        @Field("progress_id") progressId: Int,
        @Field("user_id") userId: Int
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("updateProgress")
    fun editProgress(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("updateProgressMaterial")
    fun editMaterialProgress(
        @PartMap data: MutableMap<String, RequestBody>
    ): Call<GenericSimpleResponse>

    @Multipart
    @POST("progressDone")
    fun markProgressDone(
        @PartMap submissionData: MutableMap<String, RequestBody>,
        @Part photo: ArrayList<MultipartBody.Part?>
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("approveMaterialRequest")
    fun approveMaterialRequest(
        @Field("progress_id") progressId: Int,
        @Field("user_id") userId: Int
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("statusHold")
    fun holdIssue(
        @Field("case_id") caseId: Int,
        @Field("user_id") userId: Int,
        @Field("description") description: String
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("statusResume")
    fun resumeIssue(
        @Field("case_id") caseId: Int,
        @Field("user_id") userId: Int,
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("markReadyForTrial")
    fun markAsReadyForTrial(
        @Field("case_id") caseId: Int,
        @Field("user_id") userId: Int
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("getTrial")
    fun getTrial(
        @Field("case_id") caseId: Int
    ): Call<SubmissionTrialResponse>

    @FormUrlEncoded
    @POST("startTrial")
    fun startTrial(
        @Field("case_id") caseId: Int,
        @Field("user_id") userId: Int
    ): Call<GenericSimpleResponse>

    @FormUrlEncoded
    @POST("reportTrial")
    fun reportTrial(
        @Field("case_id") caseId: Int,
        @Field("user_id") userId: Int,
        @Field("status") status: Int,
        @Field("description") description: String
    ): Call<CreationResponse>

    @FormUrlEncoded
    @POST("markIssueAsDone")
    fun markIssueDone(
        @Field("case_id") caseId: Int,
        @Field("user_id") userId: Int
    ): Call<GenericSimpleResponse>
}