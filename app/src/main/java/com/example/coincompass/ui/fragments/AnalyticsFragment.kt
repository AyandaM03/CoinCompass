package com.example.coincompass.ui.fragments

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.CategorySummary
import com.example.coincompass.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    
    private var startDate: String = ""
    private var endDate: String = ""
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())
        
        setupBarChart()
        setupListeners()
        
        // Initial load: 7 days
        updatePeriod(Period.LAST_7_DAYS)
    }

    private fun setupBarChart() {
        binding.categoryBarChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setTouchEnabled(true)
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            setScaleEnabled(false)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.BLACK
                granularity = 1f
                labelRotationAngle = -45f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                textColor = Color.BLACK
                axisMinimum = 0f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = false
            animateY(1000)

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: Highlight?) {
                    if (e != null) {
                        val category = xAxis.valueFormatter.getFormattedValue(e.x, xAxis)
                        Toast.makeText(requireContext(), "$category: R${"%.2f".format(e.y)}", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onNothingSelected() {}
            })
        }
    }

    private fun setupListeners() {
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_7_days -> updatePeriod(Period.LAST_7_DAYS)
                R.id.chip_30_days -> updatePeriod(Period.LAST_30_DAYS)
                R.id.chip_3_months -> updatePeriod(Period.LAST_3_MONTHS)
                R.id.chip_custom -> showCustomDateRangePicker()
            }
        }
    }

    private fun updatePeriod(period: Period) {
        val calendar = Calendar.getInstance()
        endDate = sdf.format(calendar.time)
        
        when (period) {
            Period.LAST_7_DAYS -> calendar.add(Calendar.DAY_OF_YEAR, -7)
            Period.LAST_30_DAYS -> calendar.add(Calendar.DAY_OF_YEAR, -30)
            Period.LAST_3_MONTHS -> calendar.add(Calendar.MONTH, -3)
            else -> {}
        }
        
        startDate = sdf.format(calendar.time)
        fetchData()
    }

    private fun showCustomDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select date range")
            .setSelection(Pair(MaterialDatePicker.todayInUtcMilliseconds(), MaterialDatePicker.todayInUtcMilliseconds()))
            .build()
            
        picker.addOnPositiveButtonClickListener { range ->
            startDate = sdf.format(Date(range.first))
            endDate = sdf.format(Date(range.second))
            fetchData()
        }
        picker.show(childFragmentManager, "CUSTOM_RANGE")
    }

    private fun fetchData() {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.chartCard.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.insightsCard.visibility = View.GONE

        // Query summaries
        db.expenseDao().getCategorySummaries(startDate, endDate).observe(viewLifecycleOwner) { summaries ->
            binding.loadingIndicator.visibility = View.GONE
            if (summaries.isNullOrEmpty()) {
                binding.emptyStateLayout.visibility = View.VISIBLE
            } else {
                binding.chartCard.visibility = View.VISIBLE
                binding.insightsCard.visibility = View.VISIBLE
                updateUI(summaries)
            }
        }
    }

    private fun updateUI(summaries: List<CategorySummary>) {
        val entries = summaries.mapIndexed { index, summary ->
            BarEntry(index.toFloat(), summary.totalAmount.toFloat())
        }

        val dataSet = BarDataSet(entries, "Spending")
        // Use CoinCompass Analytics Blue
        dataSet.color = Color.parseColor("#1976D2") 
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f
        dataSet.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "R${"%.0f".format(value)}"
            }
        }

        binding.categoryBarChart.apply {
            data = BarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(summaries.map { it.categoryName })
            notifyDataSetChanged()
            invalidate()
            animateY(800)
        }

        // Insights
        val topCategory = summaries.maxByOrNull { it.totalAmount }
        binding.topCategoryText.text = topCategory?.categoryName ?: "--"
        binding.totalPeriodAmount.text = "R${"%.2f".format(summaries.sumOf { it.totalAmount })}"
    }

    enum class Period {
        LAST_7_DAYS, LAST_30_DAYS, LAST_3_MONTHS, CUSTOM
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
