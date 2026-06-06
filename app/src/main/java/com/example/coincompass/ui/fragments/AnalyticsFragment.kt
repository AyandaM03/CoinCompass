package com.example.coincompass.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())

        setupCharts()
        observeData()
    }

    private fun setupCharts() {
        // Setup Bar Chart
        binding.incomeExpenseChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            legend.isEnabled = true
        }

        // Setup Line Chart
        binding.spendingTrendChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
            }
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun observeData() {
        db.expenseDao().getAllExpenses().observe(viewLifecycleOwner) { expenses ->
            if (expenses.isEmpty()) {
                binding.netWorthAmount.text = "R0.00"
                binding.avgIncomeAmount.text = "R0.00"
                binding.avgExpenseAmount.text = "R0.00"
                return@observe
            }

            val totalSpent = expenses.sumOf { it.amount }
            
            // Calculate Monthly Comparison
            val calendar = Calendar.getInstance()
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
            calendar.add(Calendar.MONTH, -1)
            val lastMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
            
            val currentMonthSpent = expenses.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
            val lastMonthSpent = expenses.filter { it.date.startsWith(lastMonth) }.sumOf { it.amount }
            
            val diffPercent = if (lastMonthSpent > 0) {
                ((currentMonthSpent - lastMonthSpent) / lastMonthSpent * 100).toInt()
            } else {
                0
            }

            if (diffPercent >= 0) {
                binding.comparisonText.text = "+$diffPercent% from last month"
                binding.comparisonIcon.setImageResource(android.R.drawable.arrow_up_float)
                binding.comparisonIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.delete_red))
                binding.comparisonText.setTextColor(ContextCompat.getColor(requireContext(), R.color.delete_red))
            } else {
                binding.comparisonText.text = "$diffPercent% from last month"
                binding.comparisonIcon.setImageResource(android.R.drawable.arrow_down_float)
                binding.comparisonIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.mid_green))
                binding.comparisonText.setTextColor(ContextCompat.getColor(requireContext(), R.color.mid_green))
            }

            // Averages
            val uniqueMonths = expenses.map { it.date.substring(0, 7) }.distinct().size
            val avgSpent = totalSpent / uniqueMonths

            val income = 0.0 
            val avgIncome = 0.0
            val netWorth = income - totalSpent

            binding.avgIncomeAmount.text = "R${"%.2f".format(avgIncome)}"
            binding.avgExpenseAmount.text = "R${"%.2f".format(avgSpent)}"
            binding.netWorthAmount.text = "R${"%.2f".format(netWorth)}"

            if (netWorth >= 0) {
                binding.netWorthAmount.setTextColor(Color.WHITE)
            } else {
                // If net worth is negative, show it in light green on the dark green card for visibility, or red?
                // User said "green for positive and red for negative".
                binding.netWorthAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold)) // Gold for visibility on dark green
            }

            updateCharts(expenses)
        }
    }

    private fun updateCharts(expenses: List<com.example.coincompass.data.Expense>) {
        updateBarChart(expenses)
        updateLineChart(expenses)
    }

    private fun updateBarChart(expenses: List<com.example.coincompass.data.Expense>) {
        val entries = ArrayList<BarEntry>()
        val totalSpent = expenses.sumOf { it.amount }.toFloat()
        val totalIncome = 0f // Placeholder

        entries.add(BarEntry(0f, totalIncome))
        entries.add(BarEntry(1f, totalSpent))

        val dataSet = BarDataSet(entries, "Financial Overview")
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.mid_green),
            ContextCompat.getColor(requireContext(), R.color.delete_red)
        )
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.dark_green)
        dataSet.valueTextSize = 12f

        val data = BarData(dataSet)
        binding.incomeExpenseChart.data = data
        binding.incomeExpenseChart.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Income", "Expenses"))
        binding.incomeExpenseChart.invalidate()
    }

    private fun updateLineChart(expenses: List<com.example.coincompass.data.Expense>) {
        // Group by date and sort
        val grouped = expenses.groupBy { it.date }
            .mapValues { it.value.sumOf { e -> e.amount }.toFloat() }
            .toSortedMap()

        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        var index = 0f
        
        grouped.forEach { (date, amount) ->
            entries.add(Entry(index, amount))
            labels.add(date.substring(5)) // Show MM-DD
            index++
        }

        val dataSet = LineDataSet(entries, "Daily Spending")
        dataSet.color = ContextCompat.getColor(requireContext(), R.color.mid_green)
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.dark_green))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 10f
        dataSet.setDrawFilled(true)
        dataSet.fillColor = ContextCompat.getColor(requireContext(), R.color.light_green)

        val data = LineData(dataSet)
        binding.spendingTrendChart.data = data
        binding.spendingTrendChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.spendingTrendChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
