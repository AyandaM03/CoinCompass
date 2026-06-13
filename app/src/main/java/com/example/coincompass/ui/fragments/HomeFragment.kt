package com.example.coincompass.ui.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.CategorySummary
import com.example.coincompass.databinding.FragmentHomeBinding
import com.example.coincompass.databinding.ItemBudgetCategoryBinding
import com.example.coincompass.ui.*
import com.google.android.material.appbar.AppBarLayout
import java.util.Calendar
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())

        setupHeader()
        setupQuickActions()
        setupBudgetOverview()
        
        binding.budgetHealthCard.alpha = 0f
        binding.budgetHealthCard.scaleX = 0.9f
        binding.budgetHealthCard.scaleY = 0.9f

        observeData()
    }

    private fun setupHeader() {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentUserName = sharedPref.getString("current_username", "User")
        binding.greetingText.text = "Hello, $currentUserName"

        binding.btnLogout.setOnClickListener {
            val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            sharedPref.edit()
                .remove("remember_me")
                .remove("current_username")
                .apply()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        // Handle sticky header scroll effect
        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) return@OnOffsetChangedListener
            
            val percentage = abs(verticalOffset).toFloat() / totalScrollRange
            
            if (percentage > 0.8f) {
                // Header is collapsed (black state as requested)
                binding.greetingText.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold_accent))
                // binding.financialGpsText.setTextColor(Color.WHITE)
                binding.notificationBadge.setColorFilter(Color.WHITE)
            } else {
                // Header is expanded
                binding.greetingText.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_text))
                // binding.financialGpsText.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
                binding.notificationBadge.setColorFilter(ContextCompat.getColor(requireContext(), R.color.dark_text))
            }
        })
    }

    private fun setupQuickActions() {
        // Categories - Teal
        binding.btnCategories.actionLabel.text = "Categories"
        binding.btnCategories.actionIcon.setImageResource(R.drawable.ic_categories)
        binding.btnCategories.actionCard.setCardBackgroundColor(Color.parseColor("#E0F2F1"))
        binding.btnCategories.actionIcon.setColorFilter(Color.parseColor("#008080"))
        binding.btnCategories.actionCard.setOnClickListener {
            startActivity(Intent(requireContext(), AddCategoryActivity::class.java))
        }

        // Goals - Gold
        binding.btnGoals.actionLabel.text = "Goals"
        binding.btnGoals.actionIcon.setImageResource(R.drawable.ic_goals)
        binding.btnGoals.actionCard.setCardBackgroundColor(Color.parseColor("#FFF8E1"))
        binding.btnGoals.actionIcon.setColorFilter(Color.parseColor("#FFD700"))
        binding.btnGoals.actionCard.setOnClickListener {
            startActivity(Intent(requireContext(), SetGoalActivity::class.java))
        }

        // Add - Green
        binding.btnAdd.actionLabel.text = "Add"
        binding.btnAdd.actionIcon.setImageResource(R.drawable.ic_add)
        binding.btnAdd.actionCard.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
        binding.btnAdd.actionIcon.setColorFilter(Color.parseColor("#2E7D32"))
        binding.btnAdd.actionCard.setOnClickListener {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }

        // Analytics - Blue
        binding.btnAnalytics.actionLabel.text = "Analytics"
        binding.btnAnalytics.actionIcon.setImageResource(R.drawable.ic_analytics)
        binding.btnAnalytics.actionCard.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
        binding.btnAnalytics.actionIcon.setColorFilter(Color.parseColor("#1976D2"))
        binding.btnAnalytics.actionCard.setOnClickListener {
            findNavController().navigate(R.id.nav_analytics)
        }

        // History - Coral
        binding.btnHistory.actionLabel.text = "History"
        binding.btnHistory.actionIcon.setImageResource(R.drawable.ic_history)
        binding.btnHistory.actionCard.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
        binding.btnHistory.actionIcon.setColorFilter(Color.parseColor("#D32F2F"))
        binding.btnHistory.actionCard.setOnClickListener {
            findNavController().navigate(R.id.nav_calendar)
        }
    }

    private fun setupBudgetOverview() {
        binding.budgetRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.budgetRecycler.adapter = BudgetAdapter()

        binding.btnSeeAllBudgets.setOnClickListener {
            findNavController().navigate(R.id.nav_budgets)
        }
    }

    private fun observeData() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val currentMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
        val startDate = "$year-${month.toString().padStart(2, '0')}-01"
        val endDate = "$year-${month.toString().padStart(2, '0')}-31"

        db.expenseDao().getCategorySummaries(startDate, endDate).observe(viewLifecycleOwner) { summaries ->
            (binding.budgetRecycler.adapter as? BudgetAdapter)?.submitList(summaries)
            
            val totalSpent = summaries.sumOf { it.totalAmount }
            binding.expenseAmount.text = "R${"%.2f".format(totalSpent)}"
            
            val income = 0.00
            binding.totalBalance.text = "R${"%.2f".format(income - totalSpent)}"
            binding.incomeAmount.text = "R${"%.2f".format(income)}"
            
            val savingsRate = if (income > 0) ((income - totalSpent) / income * 100).toInt() else 0
            binding.savingsRate.text = "$savingsRate%"

            // Update Budget Health with spending info
            db.goalDao().getGoalForMonth(currentMonth).observe(viewLifecycleOwner) { goal ->
                updateBudgetHealth(totalSpent, goal)
            }
        }

        db.goalDao().getGoalForMonth(currentMonth).observe(viewLifecycleOwner) { goal ->
            if (goal != null) {
                binding.savingsCard.visibility = View.VISIBLE
                binding.activeGoalName.text = "Monthly Goal"
                binding.savingsGoalPercent.text = "Target Set"
            } else {
                binding.savingsCard.visibility = View.GONE
            }
        }
    }

    private fun updateBudgetHealth(totalSpent: Double, goal: com.example.coincompass.data.Goal?) {
        if (goal == null) {
            binding.budgetHealthCard.visibility = View.GONE
            return
        }
        binding.budgetHealthCard.visibility = View.VISIBLE
        
        val minGoal = goal.minGoal
        val maxGoal = goal.maxGoal
        binding.goalRangeText.text = "Goal Range: R${"%.0f".format(minGoal)} - R${"%.0f".format(maxGoal)}"
        binding.spendingSummaryText.text = "R${"%.2f".format(totalSpent)} spent this month"

        if (maxGoal <= 0) return

        val percent = ((totalSpent / maxGoal) * 100).toInt()
        binding.healthProgress.progress = percent.coerceAtMost(100)
        binding.healthPercentText.text = "$percent%"

        when {
            totalSpent <= minGoal -> {
                binding.budgetStatusText.text = "On Track"
                binding.budgetStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
                binding.healthProgress.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
            }
            totalSpent <= maxGoal -> {
                binding.budgetStatusText.text = "Approaching Limit"
                binding.budgetStatusText.setTextColor(Color.parseColor("#F9A825")) // Gold
                binding.healthProgress.setIndicatorColor(Color.parseColor("#F9A825"))
            }
            else -> {
                binding.budgetStatusText.text = "Over Budget"
                binding.budgetStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red))
                binding.healthProgress.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.expense_red))
            }
        }

        // Animate card entry
        if (binding.budgetHealthCard.visibility == View.VISIBLE && binding.budgetHealthCard.alpha == 0f) {
            binding.budgetHealthCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(400).start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class BudgetAdapter : RecyclerView.Adapter<BudgetAdapter.ViewHolder>() {
        private var list: List<CategorySummary> = emptyList()

        fun submitList(newList: List<CategorySummary>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemBudgetCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.binding.categoryName.text = item.categoryName
            
            val limit = 5000.0
            holder.binding.categoryLimitInfo.text = "Spent R${"%.2f".format(item.totalAmount)} of R${"%.2f".format(limit)}"
            
            val percent = (item.totalAmount / limit * 100).toInt().coerceAtMost(100)
            holder.binding.categoryPercent.text = "$percent%"
            holder.binding.categoryProgress.progress = percent
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(val binding: ItemBudgetCategoryBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
