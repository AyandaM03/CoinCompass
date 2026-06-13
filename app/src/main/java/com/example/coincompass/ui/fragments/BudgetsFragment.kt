package com.example.coincompass.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Category
import com.example.coincompass.data.CategorySummary
import com.example.coincompass.databinding.FragmentBudgetsBinding
import com.example.coincompass.databinding.ItemCategoryBudgetBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.TextView
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class BudgetsFragment : Fragment() {

    private var _binding: FragmentBudgetsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var adapter: CategoryBudgetAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBudgetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())

        setupRecyclerView()
        observeData()

        binding.btnEditBudget.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), com.example.coincompass.ui.AddCategoryActivity::class.java))
        }

        binding.fabAddBudget.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), com.example.coincompass.ui.SetGoalActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = CategoryBudgetAdapter()
        binding.categoryBudgetRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryBudgetRecycler.adapter = adapter
    }

    private fun observeData() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val currentMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        val startDate = "$year-${month.toString().padStart(2, '0')}-01"
        val endDate = "$year-${month.toString().padStart(2, '0')}-31"

        db.categoryDao().getAllCategories().observe(viewLifecycleOwner) { categories ->
            db.expenseDao().getCategorySummaries(startDate, endDate).observe(viewLifecycleOwner) { summaries ->
                adapter.setData(categories, summaries)
                updateCharts(summaries)
                updateInsights(summaries, currentMonthStr)
            }
        }
    }

    private fun updateInsights(summaries: List<CategorySummary>, currentMonthStr: String) {
        val totalSpent = summaries.sumOf { it.totalAmount }
        binding.totalSpentText.text = "R${"%.2f".format(totalSpent)}"

        // Highest and Lowest
        if (summaries.isNotEmpty()) {
            val highest = summaries.maxByOrNull { it.totalAmount }
            val lowest = summaries.minByOrNull { it.totalAmount }
            binding.highestSpendingText.text = highest?.categoryName ?: "--"
            binding.lowestSpendingText.text = lowest?.categoryName ?: "--"
        } else {
            binding.highestSpendingText.text = "--"
            binding.lowestSpendingText.text = "--"
        }

        db.goalDao().getGoalForMonth(currentMonthStr).observe(viewLifecycleOwner) { goal ->
            val totalBudget = goal?.maxGoal ?: 0.0
            val remaining = totalBudget - totalSpent
            binding.remainingText.text = "R${"%.2f".format(remaining.coerceAtLeast(0.0))}"

            val percent = if (totalBudget > 0) (totalSpent / totalBudget * 100).toInt() else 0
            binding.overallProgress.progress = percent.coerceAtMost(100)
            binding.percentText.text = "$percent%"
            
            updateOverallStatusColor(percent)
        }
    }

    private fun updateCharts(summaries: List<CategorySummary>) {
        if (summaries.isEmpty()) {
            binding.chartCard.visibility = View.GONE
            return
        }
        binding.chartCard.visibility = View.VISIBLE

        val totalAmount = summaries.sumOf { it.totalAmount }.toFloat()
        val entries = summaries.map { PieEntry(it.totalAmount.toFloat(), it.categoryName) }
        val dataSet = PieDataSet(entries, "")
        
        // Custom Category Colors
        val colors = summaries.map { summary ->
            when (summary.categoryName.lowercase()) {
                "entertainment" -> Color.parseColor("#F4C542") // Gold
                "food" -> Color.parseColor("#146C43") // Green
                "income" -> Color.parseColor("#3B82F6") // Blue
                "health" -> Color.parseColor("#0D9488") // Teal
                "transport" -> Color.parseColor("#FBBF24") // Amber
                "education" -> Color.parseColor("#3B82F6") // Blue
                else -> {
                    // Generate a unique color based on hash
                    val h = abs(summary.categoryName.hashCode() % 360).toFloat()
                    Color.HSVToColor(floatArrayOf(h, 0.6f, 0.9f))
                }
            }
        }
        
        dataSet.colors = colors
        dataSet.sliceSpace = 4f
        dataSet.selectionShift = 10f
        dataSet.setDrawValues(false) // Hide labels inside
        
        val data = PieData(dataSet)
        binding.spendingPieChart.apply {
            this.data = data
            description.isEnabled = false
            
            // Legend
            legend.isEnabled = true
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.primary_green)
            legend.textSize = 12f
            legend.form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            legend.yEntrySpace = 8f
            legend.xEntrySpace = 16f
            legend.isWordWrapEnabled = true
            
            // Donut hole
            setHoleColor(Color.WHITE)
            holeRadius = 65f
            transparentCircleRadius = 70f
            setDrawCenterText(true)
            centerText = "Tap a slice\nfor details"
            setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
            setCenterTextSize(16f)
            
            setEntryLabelColor(Color.TRANSPARENT) // Hide labels
            
            animateY(1400, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
            
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: Highlight?) {
                    if (e is PieEntry) {
                        val percentage = (e.value / totalAmount * 100).toInt()
                        showSpendingDetailBottomSheet(e.label, e.value.toDouble(), percentage)
                    }
                }
                override fun onNothingSelected() {}
            })
            
            invalidate()
        }
    }

    private fun showSpendingDetailBottomSheet(category: String, amount: Double, percent: Int) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.layout_spending_detail_bottom_sheet, null)
        
        view.findViewById<TextView>(R.id.bs_category_name).text = category
        view.findViewById<TextView>(R.id.bs_amount).text = "R${"%.2f".format(amount)}"
        view.findViewById<TextView>(R.id.bs_percentage).text = "$percent% of total spending"
        
        // Color indicator
        val colorView = view.findViewById<View>(R.id.bs_color_indicator)
        colorView.background.setTint(getCategoryColorBS(category))

        view.findViewById<View>(R.id.bs_close).setOnClickListener {
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }

    private fun getCategoryColorBS(name: String): Int {
        return when (name.lowercase()) {
            "entertainment" -> Color.parseColor("#F4C542")
            "food" -> Color.parseColor("#146C43")
            "income" -> Color.parseColor("#3B82F6")
            "health" -> Color.parseColor("#0D9488")
            "transport" -> Color.parseColor("#FBBF24")
            "education" -> Color.parseColor("#3B82F6")
            else -> {
                val h = abs(name.hashCode() % 360).toFloat()
                Color.HSVToColor(floatArrayOf(h, 0.6f, 0.9f))
            }
        }
    }

    private fun updateOverallStatusColor(percent: Int) {
        val color = when {
            percent < 80 -> ContextCompat.getColor(requireContext(), R.color.primary_green)
            percent < 100 -> Color.parseColor("#FBBF24") // Amber
            else -> ContextCompat.getColor(requireContext(), R.color.expense_red)
        }
        binding.overallProgress.setIndicatorColor(color)
        binding.percentText.setTextColor(color)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class CategoryBudgetAdapter : RecyclerView.Adapter<CategoryBudgetAdapter.ViewHolder>() {
        private var categories: List<Category> = emptyList()
        private var summaries: Map<String, Double> = emptyMap()

        fun setData(newCategories: List<Category>, newSummaries: List<CategorySummary>) {
            categories = newCategories
            summaries = newSummaries.associate { it.categoryName to it.totalAmount }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemCategoryBudgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val category = categories[position]
            val spent = summaries[category.name] ?: 0.0
            val budget = category.budgetAmount
            
            holder.binding.categoryName.text = category.name
            holder.binding.categorySpent.text = "R${"%.2f".format(spent)}"
            holder.binding.categoryBudget.text = "R${"%.2f".format(budget)}"
            
            val percent = if (budget > 0) (spent / budget * 100).toInt() else 0
            holder.binding.categoryProgress.progress = percent.coerceAtMost(100)
            holder.binding.percentLabel.text = "$percent%"
            
            val remaining = budget - spent
            holder.binding.categoryStatus.text = "R${"%.2f".format(remaining.coerceAtLeast(0.0))}"
            
            val statusColor = when {
                percent < 80 -> ContextCompat.getColor(requireContext(), R.color.primary_green)
                percent < 100 -> Color.parseColor("#FBBF24") // Amber
                else -> ContextCompat.getColor(requireContext(), R.color.expense_red)
            }
            
            holder.binding.categoryProgress.setIndicatorColor(statusColor)
            holder.binding.categoryStatus.setTextColor(statusColor)
            holder.binding.percentLabel.setTextColor(statusColor)
            
            holder.binding.iconBg.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_green))
        }

        override fun getItemCount() = categories.size

        inner class ViewHolder(val binding: ItemCategoryBudgetBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
