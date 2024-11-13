package com.erela.fixme.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.adapters.SubmissionRvAdapter
import com.erela.fixme.databinding.ActivitySubmissionListBinding
import com.erela.fixme.helpers.InitAPI
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.objects.DepartmentListResponse
import com.erela.fixme.objects.SubmissionListResponse
import com.erela.fixme.objects.UserData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SubmissionListActivity : AppCompatActivity(), SubmissionRvAdapter.OnSubmissionClickListener {
    private lateinit var binding: ActivitySubmissionListBinding
    private lateinit var adapter: SubmissionRvAdapter
    private lateinit var userData: UserData
    private var selectedDepartment: String = ""
    private var submissionArrayList: ArrayList<SubmissionListResponse> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubmissionListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userData = UserDataHelper(this@SubmissionListActivity).getUserData()

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
                                        R.layout.department_dropdown_item,
                                        R.id.dropdownItemText,
                                        data
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
                                                    data[position]
                                                submissionArrayList.clear()
                                                adapter.notifyDataSetChanged()
                                                getSubmissionList()
                                            }

                                            override fun onNothingSelected(p0: AdapterView<*>?) {}
                                        }
                                    Log.e("Department List", response.body().toString())
                                }
                            } else {
                                Log.e("ERROR", response.message())
                            }
                        }

                        override fun onFailure(
                            call: Call<List<DepartmentListResponse>>,
                            throwable: Throwable
                        ) {
                            loadingBar.visibility = View.GONE
                            throwable.printStackTrace()
                        }
                    })
            } catch (exception: Exception) {
                loadingBar.visibility = View.GONE
                exception.printStackTrace()
            }
        }
    }

    private fun getSubmissionList() {
        binding.apply {
            loadingBar.visibility = View.VISIBLE
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
                                        submissionArrayList.clear()
                                        for (i in 0 until response.body()!!.size) {
                                            submissionArrayList.add(response.body()!![i])
                                        }
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                            }

                            override fun onFailure(
                                call: Call<List<SubmissionListResponse>>,
                                throwable: Throwable
                            ) {
                                loadingBar.visibility = View.GONE
                                throwable.printStackTrace()
                            }
                        })
                } else {
                    loadingBar.visibility = View.GONE
                    submissionArrayList.clear()
                }
            } catch (exception: Exception) {
                loadingBar.visibility = View.GONE
                exception.printStackTrace()
            }
        }
    }

    override fun onSubmissionClick(data: SubmissionListResponse) {
        SubmissionDetailActivity.initiate(this@SubmissionListActivity, data.idGaprojects.toString())
    }
}