package com.erela.fixme.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.erela.fixme.R
import com.erela.fixme.adapters.recycler_view.LaundryHistoryAdapter
import com.erela.fixme.custom_views.CustomToast
import com.erela.fixme.databinding.ActivityLaundryHistoryBinding
import com.erela.fixme.helpers.enableEdgeToEdgeOpaqueNav
import com.erela.fixme.objects.laundry.LaundryArrivalsResponse
import com.erela.fixme.viewmodel.LaundryCheckInViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Two lists of batches: what is in the building, and what is finished.
 *
 * ONE SCREEN, ONE FLAG, because the rows are identical and only two things differ - which endpoint
 * fills them, and whether tapping one does anything. The counter's own screen already carries the
 * same kind of flag for its incoming and outgoing flows.
 *
 * BOTH LISTS OPEN THE PROCESS SCREEN. On a finished batch it has no action buttons - the `when`
 * over the status has no branch for `completed` - so what is left is exactly what somebody opening
 * a record wants: the garments, the condition pairs, and the print button.
 *
 * THE RANGE BELONGS TO HISTORY ALONE. Active batches are today's work - a date filter there could
 * only ever hide something the operator still has to deal with.
 */
class LaundryHistoryActivity : AppCompatActivity() {
    private val binding: ActivityLaundryHistoryBinding by lazy {
        ActivityLaundryHistoryBinding.inflate(layoutInflater)
    }
    private val viewModel: LaundryCheckInViewModel by viewModels()
    private lateinit var adapter: LaundryHistoryAdapter

    /**
     * Which list this screen is: work in the building, or the archive.
     *
     * NOT CALLED  - the ViewModel has a LiveData of that name, and inside 
     * the receiver would win. It would compile and read correctly, which is exactly the kind of
     * line somebody changes at 3am and breaks.
     */
    private val activeMode: Boolean by lazy { intent.getBooleanExtra(EXTRA_ACTIVE, false) }

    /** `YYYY-MM-DD`, or null for "any date" — which is the default and has to stay reachable. */
    private var from: String? = null
    private var to: String? = null

    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdgeOpaqueNav()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.apply {
            toolBar.setNavigationOnClickListener { finish() }

            swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
                rvItems.canScrollVertically(-1)
            }
            swipeRefreshLayout.setOnRefreshListener { reload() }

            toolBarTitle.setText(
                if (activeMode) R.string.laundry_in_progress else R.string.laundry_history_title
            )

            // Today's work carries no date filter: it could only hide something still to do.
            rangeRow.visibility = if (activeMode) View.GONE else View.VISIBLE

            adapter = LaundryHistoryAdapter(
                this@LaundryHistoryActivity,
                showCompletedAt = !activeMode
            ) { batch ->
                startActivity(
                    Intent(this@LaundryHistoryActivity, LaundryProcessActivity::class.java)
                        .putExtra(LaundryProcessActivity.EXTRA_ID_TRX, batch.id)
                )
            }

            rvItems.apply {
                layoutManager = LinearLayoutManager(this@LaundryHistoryActivity)
                adapter = this@LaundryHistoryActivity.adapter
            }

            fromButton.setOnClickListener { pickDate(isFrom = true) }
            toButton.setOnClickListener { pickDate(isFrom = false) }

            clearButton.setOnClickListener {
                from = null
                to = null
                renderRange()
                reload()
            }
        }

        setupObservers()
        renderRange()
        reload()
    }

    private fun setupObservers() {
        viewModel.apply {
            // Both feeds land in the same renderer. Only one of them is ever asked for, and the
            // one that is not stays silent - which is why they are two fields and not one.
            active.observe(this@LaundryHistoryActivity) { render(it) }
            history.observe(this@LaundryHistoryActivity) { render(it) }

            isLoading.observe(this@LaundryHistoryActivity) { loading ->
                binding.swipeRefreshLayout.isRefreshing = loading
            }

            error.observe(this@LaundryHistoryActivity) { message -> toast(message) }
        }
    }

    private fun render(response: LaundryArrivalsResponse) {
        if (!response.isSuccess) {
            toast(response.message)
            return
        }

        val rows = response.data.orEmpty()

        adapter.submitList(rows)
        binding.rvItems.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyContainer.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE

        binding.tvEmptyMessage.setText(
            if (activeMode) R.string.laundry_in_progress_empty else R.string.laundry_history_empty
        )

        // An animation nobody can see should not be running.
        if (rows.isEmpty()) {
            binding.emptyAnimation.playAnimation()
        } else {
            binding.emptyAnimation.pauseAnimation()
        }
    }

    private fun reload() {
        if (activeMode) viewModel.loadActiveQueue() else viewModel.loadHistory(from, to)
    }

    /**
     * The platform's own picker.
     *
     * Opens on the date already chosen, or today. Typing `2026-09-17` on a phone is how a range
     * ends up empty for a reason nobody can see.
     */
    private fun pickDate(isFrom: Boolean) {
        val calendar = Calendar.getInstance()

        (if (isFrom) from else to)?.let { existing ->
            runCatching { formatter.parse(existing) }.getOrNull()?.let { calendar.time = it }
        }

        DatePickerDialog(
            this,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                }

                if (isFrom) {
                    from = formatter.format(picked.time)
                } else {
                    to = formatter.format(picked.time)
                }

                renderRange()
                reload()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun renderRange() {
        binding.fromText.text = from ?: getString(R.string.laundry_date_from)
        binding.toText.text = to ?: getString(R.string.laundry_date_to)
        binding.clearButton.visibility =
            if (from == null && to == null) View.GONE else View.VISIBLE
    }

    companion object {
        /** True for the in-progress list, false for the archive. */
        const val EXTRA_ACTIVE = "extra.active"
    }

    private fun toast(message: String) {
        CustomToast.getInstance(this)
            .setMessage(message)
            .setFontColor(getColor(R.color.custom_toast_font_warning))
            .setBackgroundColor(getColor(R.color.custom_toast_background_warning))
            .show()
    }
}
