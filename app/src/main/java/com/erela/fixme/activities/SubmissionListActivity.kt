package com.erela.fixme.activities

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.SubmissionRvAdapter
import com.erela.fixme.bottom_sheets.SubmissionListFilterBottomSheet
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivitySubmissionListBinding
import com.erela.fixme.helpers.UserDataHelper
import com.erela.fixme.helpers.api.InitAPI
import com.erela.fixme.objects.DataItem
import com.erela.fixme.objects.DepartmentListResponse
import com.erela.fixme.objects.SubmissionListResponse
import com.erela.fixme.objects.UserData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.view.isVisible

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
    private var startDate = ""
    private var endDate = ""
    private var selectedComplexity = ""
    private var selectedDepartment: String = ""
    private var submissionArrayList: ArrayList<DataItem?>? = ArrayList()
    private var originalSubmissionArrayList: ArrayList<DataItem?>? = ArrayList()
    private var listFilteredByStatusAndComplexity: ArrayList<DataItem?> = ArrayList()
    private var isScrollToTopButtonAnimating = false

    @SuppressLint("NotifyDataSetChanged")
    private val activityResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            submissionArrayList?.clear()
            adapter.notifyDataSetChanged()
            binding.rvSubmission.setItemViewCacheSize(submissionArrayList?.size ?: 0)
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

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val searchItem = menu.findItem(R.id.action_search)
        searchItem?.isVisible = selectedDepartment.isNotEmpty()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.submission_list_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.maxWidth = Int.MAX_VALUE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filter(newText)
                return false
            }
        })
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun filter(text: String?) {
        val filteredList: ArrayList<DataItem> = ArrayList()
        if (text.isNullOrEmpty()) {
            submissionArrayList?.clear()
            submissionArrayList?.addAll(listFilteredByStatusAndComplexity)
            adapter.notifyDataSetChanged()
            binding.rvSubmission.setItemViewCacheSize(submissionArrayList?.size ?: 0)
            binding.apply {
                rvSubmission.visibility = View.VISIBLE
                emptyListContainer.visibility = View.GONE
            }
            return
        }
        for (item in listFilteredByStatusAndComplexity) {
            if (item?.nomorRequest.toString().lowercase(Locale.getDefault())
                    .contains(text.lowercase(Locale.getDefault())) ||
                item?.judulKasus?.lowercase(Locale.getDefault())
                    ?.contains(text.lowercase(Locale.getDefault())) == true ||
                item?.keterangan?.lowercase(Locale.getDefault())
                    ?.contains(text.lowercase(Locale.getDefault())) == true ||
                item?.namaUser?.lowercase(Locale.getDefault())
                    ?.contains(text.lowercase(Locale.getDefault())) == true
            ) {
                filteredList.add(item!!)
            }
        }
        if (filteredList.isEmpty()) {
            binding.apply {
                rvSubmission.visibility = View.GONE
                emptyListContainer.visibility = View.VISIBLE
            }
        }
        submissionArrayList?.clear()
        submissionArrayList?.addAll(filteredList)
        adapter.notifyDataSetChanged()
        binding.apply {
            rvSubmission.visibility = View.VISIBLE
            rvSubmission.setItemViewCacheSize(submissionArrayList?.size ?: 0)
            emptyListContainer.visibility = View.GONE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun init() {
        binding.apply {
            setSupportActionBar(toolBar)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            toolBar.setNavigationOnClickListener {
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
            rvSubmission.setItemViewCacheSize(submissionArrayList?.size ?: 0)

            rvSubmission.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    // Only update if not currently animating to prevent race conditions
                    if (!isScrollToTopButtonAnimating) {
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                        if (firstVisibleItemPosition > 0) {
                            if (!scrollToTopButton.isVisible)
                                showScrollToTopButton()
                        } else {
                            if (scrollToTopButton.isVisible)
                                hideScrollToTopButton()
                        }
                    }
                }
            })

            scrollToTopButton.setOnClickListener {
                rvSubmission.smoothScrollToPosition(0)
            }

            swipeRefresh.setOnRefreshListener {
                submissionArrayList?.clear()
                adapter.notifyDataSetChanged()
                rvSubmission.setItemViewCacheSize(submissionArrayList?.size ?: 0)
                getSubmissionList()
                swipeRefresh.isRefreshing = false
            }

            loadingManager(true)
            try {
                InitAPI.getEndpoint.getDeptList()
                    .enqueue(object : Callback<List<DepartmentListResponse>> {
                        override fun onResponse(
                            call: Call<List<DepartmentListResponse>>,
                            response: Response<List<DepartmentListResponse>>
                        ) {
                            loadingManager(false)
                            if (response.isSuccessful) {
                                if (response.body() != null) {
                                    val data: ArrayList<String> = ArrayList()
                                    data.add(
                                        if (getString(R.string.lang) == "in")
                                            "Pilih Departemen"
                                        else
                                            "Select Department"
                                    )
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
                                                invalidateOptionsMenu()
                                                if (selectedDepartment == "") {
                                                    filterListButton.visibility = View.GONE
                                                    rvSubmission.visibility = View.VISIBLE
                                                    emptyListContainer.visibility = View.GONE
                                                }
                                                submissionArrayList?.clear()
                                                adapter.notifyDataSetChanged()
                                                rvSubmission.setItemViewCacheSize(submissionArrayList?.size ?: 0)
                                                getSubmissionList()
                                            }

                                            override fun onNothingSelected(p0: AdapterView<*>?) {}
                                        }
                                }
                            } else {
                                CustomToast.getInstance(applicationContext)
                                    .setMessage(
                                        if (getString(R.string.lang) == "in")
                                            "Terjadi kesalahan, silakan coba lagi."
                                        else
                                            "Something went wrong, please try again."
                                    )
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
                            loadingManager(false)
                            CustomToast.getInstance(applicationContext)
                                .setMessage(
                                    if (getString(R.string.lang) == "in")
                                        "Terjadi kesalahan, silakan coba lagi."
                                    else
                                        "Something went wrong, please try again."
                                )
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
                loadingManager(false)
                CustomToast.getInstance(applicationContext)
                    .setMessage(
                        if (getString(R.string.lang) == "in")
                            "Terjadi kesalahan, silakan coba lagi."
                        else
                            "Something went wrong, please try again."
                    )
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
            loadingManager(true)
            filterListButton.visibility = View.GONE
            emptyListContainer.visibility = View.GONE
            try {
                if (selectedDepartment != "") {
                    InitAPI.getEndpoint.getSubmissionList(userData.id, selectedDepartment)
                        .enqueue(object : Callback<SubmissionListResponse> {
                            @SuppressLint("NotifyDataSetChanged")
                            override fun onResponse(
                                call: Call<SubmissionListResponse>,
                                response: Response<SubmissionListResponse>
                            ) {
                                loadingManager(false)
                                if (response.isSuccessful) {
                                    if (response.body() != null) {
                                        originalSubmissionArrayList?.clear()
                                        if (response.body()?.data != null)
                                            originalSubmissionArrayList?.addAll(response.body()?.data!!)
                                        if (firstInit) {
                                            filterList(
                                                response.body()!!.data,
                                                SubmissionListFilterBottomSheet.ON_PROGRESS,
                                                startDate,
                                                endDate,
                                                "All"
                                            )
                                            selectedFilter =
                                                SubmissionListFilterBottomSheet.ON_PROGRESS
                                            selectedComplexity = "All"
                                            firstInit = false
                                        } else
                                            filterList(
                                                response.body()!!.data,
                                                selectedFilter,
                                                startDate,
                                                endDate,
                                                selectedComplexity
                                            )
                                        filterListButton.visibility = View.VISIBLE
                                        filterListButton.setOnClickListener {
                                            val bottomSheet = SubmissionListFilterBottomSheet(
                                                this@SubmissionListActivity,
                                                selectedFilter,
                                                startDate,
                                                endDate,
                                                selectedComplexity,
                                                supportFragmentManager
                                            ).also {
                                                with(it) {
                                                    setOnFilterListener(object :
                                                        SubmissionListFilterBottomSheet.OnFilterListener {
                                                        override fun onFilter(
                                                            filter: Int,
                                                            selectedFilter: Int,
                                                            startDate: String,
                                                            endDate: String,
                                                            selectedComplexity: String
                                                        ) {
                                                            // Store the dates for future filtering
                                                            this@SubmissionListActivity.startDate =
                                                                startDate
                                                            this@SubmissionListActivity.endDate =
                                                                endDate
                                                            getSubmissionList()
                                                            filterList(
                                                                response.body()!!.data,
                                                                filter,
                                                                startDate,
                                                                endDate,
                                                                selectedComplexity
                                                            )
                                                            this@SubmissionListActivity.selectedFilter =
                                                                selectedFilter
                                                            this@SubmissionListActivity.selectedComplexity =
                                                                selectedComplexity
                                                        }
                                                    })
                                                }
                                            }

                                            if (bottomSheet.window != null)
                                                bottomSheet.show()
                                        }
                                    }
                                } else {
                                    loadingManager(false)
                                    CustomToast.getInstance(applicationContext)
                                        .setMessage(
                                            if (getString(R.string.lang) == "in")
                                                "Terjadi kesalahan, silakan coba lagi."
                                            else
                                                "Something went wrong, please try again."
                                        )
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
                                call: Call<SubmissionListResponse>,
                                throwable: Throwable
                            ) {
                                loadingManager(false)
                                CustomToast.getInstance(applicationContext)
                                    .setMessage(
                                        if (getString(R.string.lang) == "in")
                                            "Terjadi kesalahan, silakan coba lagi."
                                        else
                                            "Something went wrong, please try again."
                                    )
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
                    loadingManager(false)
                    submissionArrayList?.clear()
                }
            } catch (exception: Exception) {
                loadingManager(false)
                CustomToast.getInstance(applicationContext)
                    .setMessage(
                        if (getString(R.string.lang) == "in")
                            "Terjadi kesalahan, silakan coba lagi."
                        else
                            "Something went wrong, please try again."
                    )
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
    private fun filterList(
        submissionList: List<DataItem?>?,
        filter: Int,
        startDate: String,
        endDate: String,
        complexity: String
    ) {
        binding.apply {
            val filterText = SpannableStringBuilder()
            with(filterByText) {
                when (filter) {
                    -2 -> {
                        filterText.append(
                            if (getString(R.string.lang) == "in")
                                "Semua (Selesai)"
                            else
                                "All (Finished)"
                        )
                        setTextColor(getColor(R.color.black))
                    }

                    -1 -> {
                        filterText.append(
                            if (getString(R.string.lang) == "in")
                                "Semua (Belum Selesai)"
                            else
                                "All (Unfinished)"
                        )
                        setTextColor(getColor(R.color.black))
                    }

                    100 -> {
                        filterText.append(
                            if (getString(R.string.lang) == "in")
                                "Semua Kasus"
                            else
                                "All Case"
                        )
                        setTextColor(getColor(R.color.black))
                    }

                    0 -> {
                        filterText.append("Rejected")
                        setTextColor(getColor(R.color.status_rejected))
                    }

                    1 -> {
                        filterText.append("Pending")
                        setTextColor(getColor(R.color.status_pending))
                    }

                    11 -> {
                        filterText.append("Waiting")
                        setTextColor(getColor(R.color.status_waiting))
                    }

                    2 -> {
                        filterText.append("Approved")
                        setTextColor(getColor(R.color.status_approved))
                    }

                    22 -> {
                        filterText.append("Hold")
                        setTextColor(getColor(R.color.status_hold))
                    }

                    3 -> {
                        filterText.append("On Progress")
                        setTextColor(getColor(R.color.status_on_progress))
                    }

                    30 -> {
                        filterText.append("Progress Done")
                        setTextColor(getColor(R.color.status_progress_done))
                    }

                    31 -> {
                        filterText.append("On Trial")
                        setTextColor(getColor(R.color.status_on_trial))
                    }

                    4 -> {
                        filterText.append("Done")
                        setTextColor(getColor(R.color.status_done))
                    }

                    5 -> {
                        filterText.append("Canceled")
                        setTextColor(getColor(R.color.custom_toast_font_failed))
                    }
                }
                val complexityText = when (complexity) {
                    "Low" -> if (getString(R.string.lang) == "in") " (Rendah)" else " (Low)"
                    "Middle" -> if (getString(R.string.lang) == "in") " (Menengah)" else " (Middle)"
                    "High" -> if (getString(R.string.lang) == "in") " (Tinggi)" else " (High)"
                    else -> if (getString(R.string.lang) == "in") " (Semua)" else " (All)"
                }
                val startIndexOfComplexity = filterText.length
                filterText.append(complexityText)
                val endIndexOfComplexity = filterText.length
                val complexityColor = when (complexity) {
                    "Low" -> ContextCompat.getColor(
                        context,
                        R.color.custom_toast_font_success
                    )

                    "Middle" -> ContextCompat.getColor(
                        context,
                        R.color.custom_toast_font_warning
                    )

                    "High" -> ContextCompat.getColor(
                        context,
                        R.color.custom_toast_font_failed
                    )

                    else -> ContextCompat.getColor(context, R.color.black)
                }

                filterText.setSpan(
                    ForegroundColorSpan(complexityColor),
                    startIndexOfComplexity,
                    endIndexOfComplexity,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )

                text = filterText
            }
            when (filter) {
                -1 -> {
                    // All unfinished cases - apply date filter if provided
                    submissionArrayList?.clear()
                    if(submissionList != null) {
                        for (i in 0 until submissionList.size) {
                            if (submissionList[i]?.stsGaprojects == 1 || submissionList[i]?.stsGaprojects == 11
                                || submissionList[i]?.stsGaprojects == 2 || submissionList[i]?.stsGaprojects == 22
                                || submissionList[i]?.stsGaprojects == 3 || submissionList[i]?.stsGaprojects == 30
                                || submissionList[i]?.stsGaprojects == 31
                            ) {
                                if (dateChecker(
                                        submissionList[i]?.tglInput.toString(),
                                        startDate,
                                        endDate
                                    )
                                ) {
                                    if (complexity != "All") {
                                        if (submissionList[i]?.complexity == complexity.lowercase(
                                                Locale.getDefault()
                                            )
                                        )
                                            submissionArrayList?.add(submissionList[i])
                                    } else {
                                        submissionArrayList?.add(submissionList[i])
                                    }
                                }
                            }
                        }
                    }
                }

                -2 -> {
                    // All finished cases - apply date filter
                    submissionArrayList?.clear()
                    if (submissionList != null) {
                        for (i in 0 until submissionList.size) {
                            if (submissionList[i]?.stsGaprojects == 0 || submissionList[i]?.stsGaprojects == 4
                                || submissionList[i]?.stsGaprojects == 5
                            ) {
                                if (dateChecker(
                                        submissionList[i]?.tglInput.toString(),
                                        startDate,
                                        endDate
                                    )
                                ) {
                                    if (complexity != "All") {
                                        if (submissionList[i]?.complexity == complexity.lowercase(
                                                Locale.getDefault()
                                            )
                                        )
                                            submissionArrayList?.add(submissionList[i])
                                    } else {
                                        submissionArrayList?.add(submissionList[i])
                                    }
                                }
                            }
                        }
                    }
                }

                100 -> {
                    // All cases - apply date filter to all
                    submissionArrayList?.clear()
                    if (submissionList != null) {
                        for (i in 0 until submissionList.size) {
                            if (submissionList[i]?.stsGaprojects == 1 || submissionList[i]?.stsGaprojects == 11
                                || submissionList[i]?.stsGaprojects == 2 || submissionList[i]?.stsGaprojects == 22
                                || submissionList[i]?.stsGaprojects == 3 || submissionList[i]?.stsGaprojects == 30
                                || submissionList[i]?.stsGaprojects == 31
                            ) {
                                if (dateChecker(
                                        submissionList[i]?.tglInput.toString(),
                                        startDate,
                                        endDate
                                    )
                                ) {
                                    if (complexity != "All") {
                                        if (submissionList[i]?.complexity == complexity.lowercase(
                                                Locale.getDefault()
                                            )
                                        )
                                            submissionArrayList?.add(submissionList[i])
                                    } else {
                                        submissionArrayList?.add(submissionList[i])
                                    }
                                }
                            } else
                                if (dateChecker(
                                        submissionList[i]?.tglInput.toString(),
                                        startDate,
                                        endDate
                                    )
                                ) {
                                    if (complexity != "All") {
                                        if (submissionList[i]?.complexity == complexity.lowercase(
                                                Locale.getDefault()
                                            )
                                        )
                                            submissionArrayList?.add(submissionList[i])
                                    } else {
                                        submissionArrayList?.add(submissionList[i])
                                    }
                                }
                        }
                    }
                }

                else -> {
                    // Specific status filter - apply date filter if applicable
                    submissionArrayList?.clear()
                    if (submissionList != null) {
                        for (i in 0 until submissionList.size) {
                            if (submissionList[i]?.stsGaprojects == filter) {
                                if (filter == 0 || filter == 4 || filter == 5
                                ) {
                                    if (dateChecker(
                                            submissionList[i]?.tglInput.toString(),
                                            startDate,
                                            endDate
                                        )
                                    ) {
                                        if (complexity != "All") {
                                            if (submissionList[i]?.complexity == complexity.lowercase(
                                                    Locale.getDefault()
                                                )
                                            )
                                                submissionArrayList?.add(submissionList[i])
                                        } else {
                                            submissionArrayList?.add(submissionList[i])
                                        }
                                    }
                                } else {
                                    if (dateChecker(
                                            submissionList[i]?.tglInput.toString(),
                                            startDate,
                                            endDate
                                        )
                                    ) {
                                        if (complexity != "All") {
                                            if (submissionList[i]?.complexity == complexity.lowercase(
                                                    Locale.getDefault()
                                                )
                                            )
                                                submissionArrayList?.add(submissionList[i])
                                        } else {
                                            submissionArrayList?.add(submissionList[i])
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            adapter.notifyDataSetChanged()
            rvSubmission.setItemViewCacheSize(submissionArrayList?.size ?: 0)
            listFilteredByStatusAndComplexity.clear()
            listFilteredByStatusAndComplexity.addAll(submissionArrayList!!)
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

    private fun dateChecker(date: String, startDate: String, endDate: String): Boolean {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val reportDate = simpleDateFormat.parse(date) ?: return false

        // If no date filter is selected, accept all dates
        if (startDate.isEmpty() && endDate.isEmpty()) {
            return true
        }

        // Convert report date to yyyy-MM-dd format for comparison
        val reportDateOnly = dateOnlyFormat.format(reportDate)

        // The startDate and endDate are already in yyyy-MM-dd format from the filter
        val start = startDate.ifEmpty { "1900-01-01" }
        val end = endDate.ifEmpty { "2100-12-31" }

        Log.e(
            "DateChecker",
            "Comparing reportDate: $reportDateOnly with start: $start and end: $end"
        )
        return reportDateOnly in start..end
    }

    override fun onSubmissionClick(data: DataItem?) {
        activityResultLauncher.launch(
            SubmissionDetailActivity.initiate(
                this@SubmissionListActivity,
                data?.idGaprojects.toString()
            )
        )
    }

    private fun loadingManager(isLoading: Boolean) {
        binding.apply {
            if (isLoading) {
                shimmerLayout.apply {
                    visibility = View.VISIBLE
                    startShimmer()
                }
            } else {
                shimmerLayout.apply {
                    stopShimmer()
                    visibility = View.GONE
                }
            }
        }
    }

    private fun showScrollToTopButton() {
        binding.apply {
            if (!scrollToTopButton.isVisible) {
                // Cancel any existing animation
                scrollToTopButton.animate().cancel()
                isScrollToTopButtonAnimating = true

                scrollToTopButton.visibility = View.VISIBLE
                scrollToTopButton.alpha = 0f
                scrollToTopButton.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .setListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationEnd(animation: Animator) {
                            isScrollToTopButtonAnimating = false
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            isScrollToTopButtonAnimating = false
                        }

                        override fun onAnimationRepeat(animation: Animator) {}
                    })
                    .start()
            }
        }
    }

    private fun hideScrollToTopButton() {
        binding.apply {
            if (scrollToTopButton.isVisible) {
                // Cancel any existing animation
                scrollToTopButton.animate().cancel()
                isScrollToTopButtonAnimating = true

                scrollToTopButton.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .setListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationEnd(animation: Animator) {
                            scrollToTopButton.visibility = View.GONE
                            isScrollToTopButtonAnimating = false
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            isScrollToTopButtonAnimating = false
                        }

                        override fun onAnimationRepeat(animation: Animator) {}
                    })
                    .start()
            }
        }
    }
}