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
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
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
        setupChallengeCard()
        
        binding.budgetHealthCard.alpha = 0f
        binding.budgetHealthCard.scaleX = 0.9f
        binding.budgetHealthCard.scaleY = 0.9f

        observeData()
    }

    private fun setupChallengeCard() {
        binding.btnPlayChallenge.setOnClickListener {
            startActivity(Intent(requireContext(), GameActivity::class.java))
        }
        
        binding.challengeCard.setOnClickListener {
            startActivity(Intent(requireContext(), RewardsActivity::class.java))
        }
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

        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            if (totalScrollRange == 0) return@OnOffsetChangedListener
            
            val percentage = abs(verticalOffset).toFloat() / totalScrollRange
            
            if (percentage > 0.8f) {
                binding.greetingText.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold_accent))
                binding.notificationBadge.setColorFilter(Color.WHITE)
            } else {
                binding.greetingText.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_text))
                binding.notificationBadge.setColorFilter(ContextCompat.getColor(requireContext(), R.color.dark_text))
            }
        })
    }

    private fun setupQuickActions() {
        binding.btnCategories.actionLabel.text = "Budget"
        binding.btnCategories.actionIcon.setImageResource(R.drawable.ic_categories)
        binding.btnCategories.actionCard.setCardBackgroundColor(Color.parseColor("#E0F2F1"))
        binding.btnCategories.actionIcon.setColorFilter(Color.parseColor("#008080"))
        binding.btnCategories.actionCard.setOnClickListener {
            findNavController().navigate(R.id.nav_budgets)
        }

        binding.btnGoals.actionLabel.text = "Goals"
        binding.btnGoals.actionIcon.setImageResource(R.drawable.ic_goals)
        binding.btnGoals.actionCard.setCardBackgroundColor(Color.parseColor("#FFF8E1"))
        binding.btnGoals.actionIcon.setColorFilter(Color.parseColor("#FFD700"))
        binding.btnGoals.actionCard.setOnClickListener {
            findNavController().navigate(R.id.nav_savings)
        }

        binding.btnAdd.actionLabel.text = "Add"
        binding.btnAdd.actionIcon.setImageResource(R.drawable.ic_add)
        binding.btnAdd.actionCard.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
        binding.btnAdd.actionIcon.setColorFilter(Color.parseColor("#2E7D32"))
        binding.btnAdd.actionCard.setOnClickListener {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }

        binding.btnAnalytics.actionLabel.text = "Analytics"
        binding.btnAnalytics.actionIcon.setImageResource(R.drawable.ic_analytics)
        binding.btnAnalytics.actionCard.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
        binding.btnAnalytics.actionIcon.setColorFilter(Color.parseColor("#1976D2"))
        binding.btnAnalytics.actionCard.setOnClickListener {
            findNavController().navigate(R.id.nav_analytics)
        }

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
            triggerHealthScoreUpdate()
        }

        db.expenseDao().getExpensesBetweenDates(startDate, endDate).observe(viewLifecycleOwner) { expenses ->
            updateMonthlyFinancialSummary(expenses, currentMonth)
            triggerHealthScoreUpdate()
        }

        db.goalDao().getGoalForMonth(currentMonth).observe(viewLifecycleOwner) { goal ->
            if (goal != null) {
                binding.savingsCard.visibility = View.VISIBLE
                binding.activeGoalName.text = "Monthly Goal"
                binding.savingsGoalPercent.text = "Target Set"
            } else {
                binding.savingsCard.visibility = View.GONE
            }
            triggerHealthScoreUpdate()
        }

        db.savingsGoalDao().getAllSavingsGoals().observe(viewLifecycleOwner) {
            triggerHealthScoreUpdate()
        }

        db.rewardPointsDao().getRewardPoints().observe(viewLifecycleOwner) { stats ->
            if (stats != null) {
                binding.dashboardPoints.text = stats.points.toString()
                binding.dashboardLevel.text = stats.level.toString()
                binding.dashboardGames.text = stats.totalGamesPlayed.toString()
            }
        }
    }

    private fun triggerHealthScoreUpdate() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val currentMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
        val startDate = "$year-${month.toString().padStart(2, '0')}-01"
        val endDate = "$year-${month.toString().padStart(2, '0')}-31"

        lifecycleScope.launch {
            val expenses = db.expenseDao().getAllExpensesList()
            val summaries = db.expenseDao().getCategorySummaries(startDate, endDate).value
            val monthlyGoal = db.goalDao().getGoalForMonth(currentMonth).value
            val savingsGoals = db.savingsGoalDao().getAllSavingsGoals().value
            
            calculateFinancialHealthScore(expenses, summaries, monthlyGoal, savingsGoals)
        }
    }

    private fun calculateFinancialHealthScore(
        expenses: List<com.example.coincompass.data.Expense>?,
        summaries: List<com.example.coincompass.data.CategorySummary>?,
        monthlyGoal: com.example.coincompass.data.Goal?,
        savingsGoals: List<com.example.coincompass.data.SavingsGoal>?
    ) {
        if (_binding == null) return
        
        var score = 0
        val totalSpent = expenses?.filter { it.type == "Expense" }?.sumOf { it.amount } ?: 0.0
        
        if (monthlyGoal != null && monthlyGoal.maxGoal > 0) {
            score += when {
                totalSpent <= monthlyGoal.minGoal -> 30
                totalSpent <= monthlyGoal.maxGoal -> 20
                totalSpent <= monthlyGoal.maxGoal * 1.1 -> 10
                else -> 0
            }
        } else {
            score += 15
        }
        
        if (!savingsGoals.isNullOrEmpty()) {
            val avgProgress = savingsGoals.map { 
                if (it.targetAmount > 0) (it.currentAmount / it.targetAmount * 100).toInt() else 0 
            }.average()
            score += (avgProgress * 0.3).toInt().coerceIn(0, 30)
        } else {
            score += 10
        }
        
        val transactionCount = expenses?.size ?: 0
        score += when {
            transactionCount >= 20 -> 20
            transactionCount >= 10 -> 15
            transactionCount >= 5 -> 10
            else -> 5
        }
        
        if (!summaries.isNullOrEmpty()) {
            val categoriesUnderBudget = summaries.count { it.totalAmount < 5000 }
            val adherenceRatio = categoriesUnderBudget.toDouble() / summaries.size
            score += (adherenceRatio * 20).toInt()
        } else {
            score += 10
        }
        
        val finalScore = score.coerceIn(0, 100)
        binding.scoreText.text = "$finalScore"
        binding.scoreProgress.progress = finalScore
        
        val status: String
        val color: Int
        when {
            finalScore >= 90 -> {
                status = "Excellent"
                color = ContextCompat.getColor(requireContext(), R.color.primary_green)
            }
            finalScore >= 70 -> {
                status = "Good"
                color = ContextCompat.getColor(requireContext(), R.color.gold_accent)
            }
            finalScore >= 50 -> {
                status = "Fair"
                color = Color.parseColor("#FB8C00")
            }
            else -> {
                status = "Needs Improvement"
                color = ContextCompat.getColor(requireContext(), R.color.expense_red)
            }
        }
        
        binding.scoreStatusText.text = status
        binding.scoreStatusText.setTextColor(color)
        binding.scoreProgress.setIndicatorColor(color)
        
        binding.financialHealthScoreCard.animate()
            .scaleX(1.02f).scaleY(1.02f)
            .setDuration(150)
            .withEndAction {
                binding.financialHealthScoreCard.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()
    }

    private fun updateMonthlyFinancialSummary(expenses: List<com.example.coincompass.data.Expense>?, currentMonth: String) {
        if (_binding == null) return
        val transactions = expenses ?: emptyList()
        val income = transactions.filter { it.type == "Income" }.sumOf { it.amount }
        val expensesAmount = transactions.filter { it.type == "Expense" }.sumOf { it.amount }
        val saved = (income - expensesAmount).coerceAtLeast(0.0)
        
        binding.totalBalance.text = "R${"%.2f".format(income - expensesAmount)}"
        binding.incomeAmount.text = "R${"%.2f".format(income)}"
        binding.expenseAmount.text = "R${"%.2f".format(expensesAmount)}"
        
        val savingsRate = if (income > 0) ((income - expensesAmount) / income * 100).toInt().coerceAtLeast(0) else 0
        binding.savingsRate.text = "$savingsRate%"

        binding.summaryIncomeText.text = "R${"%.2f".format(income)}"
        binding.summaryExpensesText.text = "R${"%.2f".format(expensesAmount)}"
        binding.summarySavedText.text = "R${"%.2f".format(saved)}"

        updateSpendingInsights(transactions)
        generateSmartTips(transactions, currentMonth)

        db.goalDao().getGoalForMonth(currentMonth).observe(viewLifecycleOwner) { goal ->
            if (goal != null) {
                updateBudgetHealth(expensesAmount, goal)
                val maxBudget = goal.maxGoal
                val remaining = (maxBudget - expensesAmount).coerceAtLeast(0.0)
                binding.summaryRemainingText.text = "R${"%.2f".format(remaining)}"

                val status: String
                val statusColor: Int
                when {
                    savingsRate >= 20 || (maxBudget > 0 && expensesAmount < maxBudget * 0.7) -> {
                        status = "Excellent"
                        statusColor = ContextCompat.getColor(requireContext(), R.color.primary_green)
                    }
                    savingsRate >= 5 || (maxBudget > 0 && expensesAmount < maxBudget * 0.95) -> {
                        status = "Good"
                        statusColor = ContextCompat.getColor(requireContext(), R.color.gold_accent)
                    }
                    else -> {
                        status = "Needs Improvement"
                        statusColor = ContextCompat.getColor(requireContext(), R.color.expense_red)
                    }
                }
                binding.summaryStatusBadge.text = status
                binding.summaryStatusBadge.setTextColor(statusColor)
                val badgeCard = binding.summaryStatusBadge.parent as? com.google.android.material.card.MaterialCardView
                badgeCard?.strokeColor = statusColor
            } else {
                binding.budgetHealthCard.visibility = View.GONE
                binding.summaryRemainingText.text = "Set Goal"
                binding.summaryStatusBadge.text = "No Goal Set"
            }
        }
    }

    private fun updateSpendingInsights(transactions: List<com.example.coincompass.data.Expense>) {
        val expenses = transactions.filter { it.type == "Expense" }
        if (expenses.isEmpty()) {
            binding.insightsGrid.visibility = View.GONE
            binding.insufficientDataText.visibility = View.VISIBLE
            return
        }
        binding.insightsGrid.visibility = View.VISIBLE
        binding.insufficientDataText.visibility = View.GONE
        
        val categoryGroups = expenses.groupBy { it.categoryName }.mapValues { it.value.sumOf { e -> e.amount } }
        val highestCat = categoryGroups.maxByOrNull { it.value }
        val lowestCat = categoryGroups.minByOrNull { it.value }
        binding.insightHighestCat.text = highestCat?.key ?: "--"
        binding.insightLowestCat.text = lowestCat?.key ?: "--"
        
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val dayFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
        val dayGroups = expenses.groupBy { 
            try { val date = sdf.parse(it.date); dayFormat.format(date!!) } catch (e: Exception) { "Unknown" }
        }.mapValues { it.value.sumOf { e -> e.amount } }
        val mostActiveDay = dayGroups.maxByOrNull { it.value }
        binding.insightActiveDay.text = mostActiveDay?.key ?: "--"
        
        val uniqueDays = expenses.map { it.date }.distinct().size
        val totalSpent = expenses.sumOf { it.amount }
        val dailyAvg = if (uniqueDays > 0) totalSpent / uniqueDays else 0.0
        binding.insightDailyAvg.text = "R${"%.2f".format(dailyAvg)}"
        
        val largestExpense = expenses.maxByOrNull { it.amount }
        binding.insightLargestExpense.text = largestExpense?.description ?: largestExpense?.categoryName ?: "--"
        binding.insightLargestAmount.text = "R${"%.2f".format(largestExpense?.amount ?: 0.0)}"
    }

    private fun generateSmartTips(transactions: List<com.example.coincompass.data.Expense>, currentMonth: String) {
        val expenses = transactions.filter { it.type == "Expense" }
        val income = transactions.filter { it.type == "Income" }.sumOf { it.amount }
        val totalSpent = expenses.sumOf { it.amount }
        val tips = mutableListOf<Pair<Int, String>>()
        
        if (expenses.isEmpty()) {
            tips.add(R.drawable.ic_analytics to "Track your first expense to get personalized financial tips!")
            tips.add(R.drawable.ic_add to "Add your monthly income to see your savings potential.")
            tips.add(R.drawable.ic_goals to "Set a monthly spending limit to keep your budget healthy.")
        } else {
            val savingsRate = if (income > 0) ((income - totalSpent) / income * 100).toInt() else 0
            when {
                savingsRate >= 30 -> tips.add(R.drawable.ic_analytics to "Outstanding! Your savings rate is $savingsRate%. You're building wealth fast.")
                savingsRate >= 15 -> tips.add(R.drawable.ic_analytics to "Great job! You've saved $savingsRate% of your income this month.")
                savingsRate > 0 -> tips.add(R.drawable.ic_analytics to "You're saving $savingsRate% of your income. Can you reach 15%?")
                else -> tips.add(R.drawable.ic_history to "Your spending exceeds your income. Consider reviewing non-essential costs.")
            }
            
            val categoryGroups = expenses.groupBy { it.categoryName }.mapValues { it.value.sumOf { e -> e.amount } }
            val topCat = categoryGroups.maxByOrNull { it.value }
            if (topCat != null && totalSpent > 0) {
                val catPercent = (topCat.value / totalSpent * 100).toInt()
                if (catPercent > 40) {
                    tips.add(R.drawable.ic_categories to "${topCat.key} accounts for $catPercent% of your spending. Try a 'no-spend' week for this category.")
                } else {
                    tips.add(R.drawable.ic_categories to "Your spending is well-distributed across categories. Nice balance!")
                }
            }

            db.goalDao().getGoalForMonth(currentMonth).observe(viewLifecycleOwner) { goal ->
                if (goal != null && goal.maxGoal > 0) {
                    val budgetUsed = (totalSpent / goal.maxGoal * 100).toInt()
                    when {
                        budgetUsed > 100 -> tips.add(R.drawable.ic_history to "You've exceeded your R${"%.0f".format(goal.maxGoal)} budget. Review your transactions.")
                        budgetUsed > 85 -> tips.add(R.drawable.ic_history to "Warning: You've used $budgetUsed% of your monthly budget limit.")
                        budgetUsed < 50 -> tips.add(R.drawable.ic_goals to "Excellent! You've used less than half of your R${"%.0f".format(goal.maxGoal)} budget.")
                        else -> tips.add(R.drawable.ic_goals to "You are staying within your budget goals. Keep it up!")
                    }
                } else {
                    tips.add(R.drawable.ic_goals to "Set a Maximum Spending Limit to get advanced budget alerts.")
                }
                updateTipsUI(tips.take(3))
            }
            return
        }
        updateTipsUI(tips.take(3))
    }

    private fun updateTipsUI(tips: List<Pair<Int, String>>) {
        if (_binding == null) return
        if (tips.size >= 1) {
            binding.tip1Layout.visibility = View.VISIBLE
            binding.tip1Icon.setImageResource(tips[0].first)
            binding.tip1Text.text = tips[0].second
        } else { binding.tip1Layout.visibility = View.GONE }
        
        if (tips.size >= 2) {
            binding.tip2Layout.visibility = View.VISIBLE
            binding.tip2Icon.setImageResource(tips[1].first)
            binding.tip2Text.text = tips[1].second
        } else { binding.tip2Layout.visibility = View.GONE }
        
        if (tips.size >= 3) {
            binding.tip3Layout.visibility = View.VISIBLE
            binding.tip3Icon.setImageResource(tips[2].first)
            binding.tip3Text.text = tips[2].second
        } else { binding.tip3Layout.visibility = View.GONE }
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
        android.animation.ObjectAnimator.ofInt(binding.healthProgress, "progress", binding.healthProgress.progress, percent.coerceAtMost(100)).setDuration(1000).start()
        binding.healthPercentText.text = "$percent%"
        when {
            totalSpent <= minGoal -> {
                binding.budgetStatusText.text = "On Track"
                binding.budgetStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
                binding.healthProgress.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
            }
            totalSpent <= maxGoal -> {
                binding.budgetStatusText.text = "Approaching Limit"
                binding.budgetStatusText.setTextColor(Color.parseColor("#F9A825"))
                binding.healthProgress.setIndicatorColor(Color.parseColor("#F9A825"))
            }
            else -> {
                binding.budgetStatusText.text = "Over Budget"
                binding.budgetStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red))
                binding.healthProgress.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.expense_red))
            }
        }
        if (binding.budgetHealthCard.alpha == 0f) {
            binding.budgetHealthCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500).setInterpolator(android.view.animation.OvershootInterpolator()).start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class BudgetAdapter : RecyclerView.Adapter<BudgetAdapter.ViewHolder>() {
        private var list: List<CategorySummary> = emptyList()
        fun submitList(newList: List<CategorySummary>) { list = newList; notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemBudgetCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
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
