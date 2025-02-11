package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.SubmissionRvAdapter
import com.erela.fixme.bottom_sheets.SubmissionListFilterBottomSheet
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivitySubmissionListBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.DepartmentListResponse
import com.erela.fixme.objects.SubmissionListResponse
import com.erela.fixme.objects.UserData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class SubmissionListActivity : AppCompatActivity(), SubmissionRvAdapter.OnSubmissionClickListener {
    private val binding: ActivitySubmissionListBinding by lazy {
        ActivitySubmissionListBinding.inflate(layoutInflater)
    }
    private val userData: UserData by lazy {
        UserDataHelper(this@SubmissionListActivity).getUserData()
    }
    private lateinit var adapter: SubmissionRvAdapter
    private var firstInit = true
    private var selectedFilter = 100
    private var selectedDepartment: String = ""
    private var submissionArrayList: ArrayList<SubmissionListResponse> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    private val activityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            submissionArrayList.clear()
            adapter.notifyDataSetChanged()
            getSubmissionList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun init() {
        binding.apply {
            backButton.setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }

            adapter = SubmissionRvAdapter(
                this@SubmissionListActivity,
                submissionArrayList
            ).also {
                it.onSubmissionClickListener(
                    this@SubmissionListActivity
                )
            }
            rvSubmission.layoutManager =
                LinearLayoutManager(applicationContext)
            rvSubmission.adapter = adapter

            swipeRefresh.setOnRefreshListener {
                submissionArrayList.clear()
                adapter.notifyDataSetChanged()
                getSubmissionList()
                swipeRefresh.isRefreshing = false
            }

            loadingBar.visibility = View.VISIBLE
            try {
                InitAPI.getAPI.getDepartmentList()
                    .enqueue(object : Callback<List<DepartmentListResponse>> {
                        override fun onResponse(
                            call: Call<List<DepartmentListResponse>>,
                            response: Response<List<DepartmentListResponse>>
                        ) {
                            loadingBar.visibility = View.GONE
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    val data: ArrayList<String> = ArrayList()
                                    data.add("Select Department")
                                    for (i in 0 until response.body()!!.size) {
                                        data.add(
                                            response.body()!![i].namaDept.toString()
                                        )
                                    }
                                    val dropdownAdapter = ArrayAdapter(
                                        this@SubmissionListActivity,
                                        R.layout.general_dropdown_item,
                                        R.id.dropdownItemText,
                                        data.distinct()
                                    )
                                    departmentDropdown.adapter = dropdownAdapter
                                    departmentDropdown.onItemSelectedListener =
                                        object : AdapterView.OnItemSelectedListener {
                                            override fun onItemSelected(
                                                parent: AdapterView<*>?,
                                                view: View?,
                                                position: Int,
                                                id: Long
                                            ) {
                                                selectedDepartment = if (position == 0)
                                                    ""
                                                else
                                                    data.distinct()[position]
                                                if (selectedDepartment == "") {
                                                    filterListButton.visibility = View.GONE
                                                    rvSubmission.visibility = View.VISIBLE
                                                    emptyListContainer.visibility = View.GONE
                                                }
                                                submissionArrayList.clear()
                                                adapter.notifyDataSetChanged()
                                                getSubmissionList()
                                            }

                                            override fun onNothingSelected(p0: AdapterView<*>?) {}
                                        }
                                }
                            } else {
                                CustomToast.getInstance(applicationContext)
                                    .setMessage("Something went wrong, please try again.")
                                    .setFontColor(
                                        ContextCompat.getColor(
                                            this@SubmissionListActivity,
                                            R.color.custom_toast_font_failed
                                        )
                                    )
                                    .setBackgroundColor(
                                        ContextCompat.getColor(
                                            this@SubmissionListActivity,
                                            R.color.custom_toast_background_failed
                                        )
                                    ).show()
                                Log.e("ERROR", response.message())
                                finish()
                            }
                        }

                        override fun onFailure(
                            call: Call<List<DepartmentListResponse>>,
                            throwable: Throwable
                        ) {
                            Log.e("ERROR", throwable.toString())
                            loadingBar.visibility = View.GONE
                            CustomToast.getInstance(applicationContext)
                                .setMessage("Something went wrong, please try again.")
                                .setFontColor(
                                    ContextCompat.getColor(
                                        this@SubmissionListActivity,
                                        R.color.custom_toast_font_failed
                                    )
                                )
                                .setBackgroundColor(
                                    ContextCompat.getColor(
                                        this@SubmissionListActivity,
                                        R.color.custom_toast_background_failed
                                    )
                                ).show()
                            throwable.printStackTrace()
                            finish()
                        }
                    })
            } catch (exception: Exception) {
                loadingBar.visibility = View.GONE
                CustomToast.getInstance(applicationContext)
                    .setMessage("Something went wrong, please try again.")
                    .setFontColor(
                        ContextCompat.getColor(
                            this@SubmissionListActivity,
                            R.color.custom_toast_font_failed
                        )
                    )
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            this@SubmissionListActivity,
                            R.color.custom_toast_background_failed
                        )
                    ).show()
                Log.e("ERROR", exception.toString())
                exception.printStackTrace()
            }
        }
    }

    private fun getSubmissionList() {
        binding.apply {
            loadingBar.visibility = View.VISIBLE
            filterListButton.visibility = View.GONE
            emptyListContainer.visibility = View.GONE
            try {
                if (selectedDepartment != "") {
                    InitAPI.getAPI.getSubmissionList(userData.id, selectedDepartment)
                        .enqueue(object : Callback<List<SubmissionListResponse>> {
                            @SuppressLint("NotifyDataSetChanged")
                            override fun onResponse(
                                call: Call<List<SubmissionListResponse>>,
                                response: Response<List<SubmissionListResponse>>
                            ) {
                                loadingBar.visibility = View.GONE
                                if (response.isSuccessful) {
                                    if (response.body() != null) {
                                        if (firstInit) {
                                            filterList(
                                                response.body(),
                                                SubmissionListFilterBottomSheet.ON_PROGRESS
                                            )
                                            selectedFilter =
                                                SubmissionListFilterBottomSheet.ON_PROGRESS
                                            firstInit = false
                                        } else
                                            filterList(response.body(), selectedFilter)
                                        filterListButton.visibility = View.VISIBLE
                                        filterListButton.setOnClickListener {
                                            val bottomSheet = SubmissionListFilterBottomSheet(
                                                this@SubmissionListActivity, selectedFilter
                                            ).also {
                                                with(it) {
                                                    setOnFilterListener(object :
                                                        SubmissionListFilterBottomSheet.OnFilterListener {
                                                        override fun onFilter(
                                                            filter: Int,
                                                            selectedFilter: Int
                                                        ) {
                                                            filterList(response.body(), filter)
                                                            this@SubmissionListActivity.selectedFilter =
                                                                selectedFilter
                                                        }
                                                    })
                                                }
                                            }

                                            if (bottomSheet.window != null)
                                                bottomSheet.show()
                                        }
                                    }
                                } else {
                                    loadingBar.visibility = View.GONE
                                    CustomToast.getInstance(applicationContext)
                                        .setMessage("Something went wrong, please try again.")
                                        .setFontColor(
                                            ContextCompat.getColor(
                                                this@SubmissionListActivity,
                                                R.color.custom_toast_font_failed
                                            )
                                        )
                                        .setBackgroundColor(
                                            ContextCompat.getColor(
                                                this@SubmissionListActivity,
                                                R.color.custom_toast_background_failed
                                            )
                                        ).show()
                                    Log.e("ERROR", response.message())
                                }
                            }

                            override fun onFailure(
                                call: Call<List<SubmissionListResponse>>,
                                throwable: Throwable
                            ) {
                                loadingBar.visibility = View.GONE
                                CustomToast.getInstance(applicationContext)
                                    .setMessage("Something went wrong, please try again.")
                                    .setFontColor(
                                        ContextCompat.getColor(
                                            this@SubmissionListActivity,
                                            R.color.custom_toast_font_failed
                                        )
                                    )
                                    .setBackgroundColor(
                                        ContextCompat.getColor(
                                            this@SubmissionListActivity,
                                            R.color.custom_toast_background_failed
                                        )
                                    ).show()
                                throwable.printStackTrace()
                            }
                        })
                } else {
                    loadingBar.visibility = View.GONE
                    submissionArrayList.clear()
                }
            } catch (exception: Exception) {
                loadingBar.visibility = View.GONE
                CustomToast.getInstance(applicationContext)
                    .setMessage("Something went wrong, please try again.")
                    .setFontColor(
                        ContextCompat.getColor(
                            this@SubmissionListActivity,
                            R.color.custom_toast_font_failed
                        )
                    )
                    .setBackgroundColor(
                        ContextCompat.getColor(
                            this@SubmissionListActivity,
                            R.color.custom_toast_background_failed
                        )
                    ).show()
                exception.printStackTrace()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun filterList(submissionList: List<SubmissionListResponse>?, filter: Int) {
        binding.apply {
            with(filterByText) {
                when (filter) {
                    -2 -> {
                        text = "All (Finished)"
                        setTextColor(getColor(R.color.black))
                    }

                    -1 -> {
                        text = "All (Unfinished)"
                        setTextColor(getColor(R.color.black))
                    }

                    100 -> {
                        text = "All Case"
                        setTextColor(getColor(R.color.black))
                    }

                    0 -> {
                        text = "Rejected"
                        setTextColor(getColor(R.color.status_rejected))
                    }

                    1 -> {
                        text = "Pending"
                        setTextColor(getColor(R.color.status_pending))
                    }

                    11 -> {
                        text = "Waiting"
                        setTextColor(getColor(R.color.status_waiting))
                    }

                    2 -> {
                        text = "Approved"
                        setTextColor(getColor(R.color.status_approved))
                    }

                    22 -> {
                        text = "Hold"
                        setTextColor(getColor(R.color.status_hold))
                    }

                    3 -> {
                        text = "On Progress"
                        setTextColor(getColor(R.color.status_on_progress))
                    }

                    30 -> {
                        text = "Progress Done"
                        setTextColor(getColor(R.color.status_progress_done))
                    }

                    31 -> {
                        text = "On Trial"
                        setTextColor(getColor(R.color.status_on_trial))
                    }

                    4 -> {
                        text = "Done"
                        setTextColor(getColor(R.color.status_done))
                    }

                    5 -> {
                        text = "Canceled"
                        setTextColor(getColor(R.color.custom_toast_font_failed))
                    }
                }
            }
            when (filter) {
                -1 -> {
                    submissionArrayList.clear()
                    for (i in 0 until submissionList!!.size) {
                        if (submissionList[i].stsGaprojects == 1 || submissionList[i].stsGaprojects == 11
                            || submissionList[i].stsGaprojects == 2 || submissionList[i].stsGaprojects == 22
                            || submissionList[i].stsGaprojects == 3 || submissionList[i].stsGaprojects == 30
                            || submissionList[i].stsGaprojects == 31
                        )
                            submissionArrayList.add(submissionList[i])
                    }
                }

                -2 -> {
                    submissionArrayList.clear()
                    for (i in 0 until submissionList!!.size) {
                        if (submissionList[i].stsGaprojects == 0 || submissionList[i].stsGaprojects == 4
                            || submissionList[i].stsGaprojects == 5
                        )
                            if (dateChecker(submissionList[i].tglInput.toString()))
                                submissionArrayList.add(submissionList[i])
                    }
                }

                100 -> {
                    submissionArrayList.clear()
                    for (i in 0 until submissionList!!.size) {
                        if (submissionList[i].stsGaprojects == 1 || submissionList[i].stsGaprojects == 11
                            || submissionList[i].stsGaprojects == 2 || submissionList[i].stsGaprojects == 22
                            || submissionList[i].stsGaprojects == 3 || submissionList[i].stsGaprojects == 30
                            || submissionList[i].stsGaprojects == 31
                        )
                            submissionArrayList.add(submissionList[i])
                        else
                            if (dateChecker(submissionList[i].tglInput.toString()))
                                submissionArrayList.add(submissionList[i])
                    }
                }

                else -> {
                    submissionArrayList.clear()
                    for (i in 0 until submissionList!!.size) {
                        if (submissionList[i].stsGaprojects == filter) {
                            if (filter == 0 || filter == 4 || filter == 5
                            ) {
                                if (dateChecker(submissionList[i].tglInput.toString()))
                                    submissionArrayList.add(submissionList[i])
                            } else
                                submissionArrayList.add(submissionList[i])
                        }
                    }
                }
            }
            adapter.notifyDataSetChanged()
            emptyListAnimation.playAnimation()
            if (adapter.itemCount == 0) {
                if (selectedDepartment == "") {
                    rvSubmission.visibility = View.VISIBLE
                    emptyListContainer.visibility = View.GONE
                } else {
                    rvSubmission.visibility = View.GONE
                    emptyListContainer.visibility = View.VISIBLE
                }
            } else {
                rvSubmission.visibility = View.VISIBLE
                emptyListContainer.visibility = View.GONE
            }
        }
    }

    private fun dateChecker(date: String): Boolean {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val thisMonth = LocalDateTime.now().month
        val thisYear = LocalDateTime.now().year
        val firstDate = Date.from(
            LocalDateTime.of(thisYear, thisMonth, 1, 0, 0).atZone(ZoneId.systemDefault())
                .toInstant()
        )
        val lastDate = Date.from(
            LocalDateTime.of(thisYear, thisMonth, LocalDate.now().lengthOfMonth(), 0, 0)
                .atZone(ZoneId.systemDefault()).toInstant()
        )
        val reportDate = simpleDateFormat.parse(date)

        return if (reportDate != null)
            reportDate.after(firstDate) && reportDate.before(lastDate)
        else false
    }

    override fun onSubmissionClick(data: SubmissionListResponse) {
        activityResultLauncher.launch(
            SubmissionDetailActivity.initiate(
                this@SubmissionListActivity,
                data.idGaprojects.toString()
            )
        )
    }
}