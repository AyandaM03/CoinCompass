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
import com.github.mikephil.charting.components.LimitLine
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
        
        // Check for navigation arguments from Calendar
        val argStart = arguments?.getString("startDate")
        val argEnd = arguments?.getString("endDate")
        
        if (!argStart.isNullOrEmpty() && !argEnd.isNullOrEmpty()) {
            startDate = argStart
            endDate = argEnd
            binding.filterChipGroup.clearCheck()
            fetchData()
        } else {
            // Initial load: 7 days
            updatePeriod(Period.LAST_7_DAYS)
        }
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
                labelRotationAngle = -30f
                yOffset = 10f
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                textColor = Color.BLACK
                axisMinimum = 0f
                xOffset = 10f
            }
            
            axisRight.isEnabled = false
            legend.isEnabled = false
            animateY(1200)

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
        
        // Specific Category Colors
        val colors = summaries.map { summary ->
            when (summary.categoryName.lowercase()) {
                "food", "groceries", "dining" -> Color.parseColor("#FB8C00") // Orange
                "transport", "travel", "fuel" -> Color.parseColor("#1E88E5") // Blue
                "entertainment", "fun", "hobbies" -> Color.parseColor("#8E24AA") // Purple
                "health", "medical", "wellness" -> Color.parseColor("#E53935") // Red
                "savings", "investment", "income" -> Color.parseColor("#43A047") // Green
                else -> Color.parseColor("#1976D2") // Default Blue
            }
        }
        
        dataSet.colors = colors
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 10f
        dataSet.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value > 0) "R${"%.0f".format(value)}" else ""
            }
        }

        binding.categoryBarChart.apply {
            val barData = BarData(dataSet)
            barData.barWidth = 0.5f // Reduced width by 50% for cleaner look
            data = barData
            
            xAxis.valueFormatter = IndexAxisValueFormatter(summaries.map { it.categoryName })
            
            // Goals Integration with new colors
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            db.goalDao().getGoalForMonth(currentMonth).observe(viewLifecycleOwner) { goal ->
                axisLeft.removeAllLimitLines()
                if (goal != null && (goal.minGoal > 0 || goal.maxGoal > 0)) {
                    binding.noGoalsMessage.visibility = View.GONE
                    
                    if (goal.minGoal > 0) {
                        val minLine = LimitLine(goal.minGoal.toFloat(), "Min Goal").apply {
                            lineWidth = 2.5f
                            lineColor = Color.parseColor("#F9A825") // Gold/Yellow
                            enableDashedLine(12f, 8f, 0f)
                            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                            textSize = 10f
                            textColor = Color.parseColor("#F9A825")
                        }
                        axisLeft.addLimitLine(minLine)
                    }

                    if (goal.maxGoal > 0) {
                        val maxLine = LimitLine(goal.maxGoal.toFloat(), "Max Limit").apply {
                            lineWidth = 2.5f
                            lineColor = ContextCompat.getColor(requireContext(), R.color.expense_red) // Red
                            enableDashedLine(12f, 8f, 0f)
                            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                            textSize = 10f
                            textColor = ContextCompat.getColor(requireContext(), R.color.expense_red)
                        }
                        axisLeft.addLimitLine(maxLine)
                    }
                } else {
                    binding.noGoalsMessage.visibility = View.VISIBLE
                }
                invalidate()
            }

            notifyDataSetChanged()
            invalidate()
            animateY(1400, com.github.mikephil.charting.animation.Easing.EaseOutBack)
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
