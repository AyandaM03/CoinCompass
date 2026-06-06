package com.example.coincompass.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Category
import com.example.coincompass.data.CategorySummary
import com.example.coincompass.databinding.FragmentBudgetsBinding
import com.example.coincompass.databinding.ItemCategoryBudgetBinding
import java.text.SimpleDateFormat
import java.util.*

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
            // Edit all categories
            startActivity(android.content.Intent(requireContext(), com.example.coincompass.ui.AddCategoryActivity::class.java))
        }

        binding.fabAddBudget.setOnClickListener {
            // Adjust overall monthly goal
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

        // Observe total monthly goal
        db.goalDao().getGoalForMonth(currentMonthStr).observe(viewLifecycleOwner) { goal ->
            val totalBudget = goal?.maxGoal ?: 0.0
            binding.monthlyBudgetText.text = "R${"%.2f".format(totalBudget)}"
            updateOverallSummary(totalBudget)
        }

        // Observe category spending and budgets
        db.categoryDao().getAllCategories().observe(viewLifecycleOwner) { categories ->
            db.expenseDao().getCategorySummaries(startDate, endDate).observe(viewLifecycleOwner) { summaries ->
                adapter.setData(categories, summaries)
                
                val totalSpent = summaries.sumOf { it.totalAmount }
                binding.totalSpentText.text = "R${"%.2f".format(totalSpent)}"
                
                // We need the latest total budget to update the summary
                db.goalDao().getGoalForMonth(currentMonthStr).observe(viewLifecycleOwner) { goal ->
                    val totalBudget = goal?.maxGoal ?: 0.0
                    val remaining = totalBudget - totalSpent
                    binding.remainingText.text = "R${"%.2f".format(remaining)}"
                    
                    val percent = if (totalBudget > 0) (totalSpent / totalBudget * 100).toInt() else 0
                    binding.overallProgress.progress = percent
                    binding.percentText.text = "$percent%"
                }
            }
        }
    }

    private fun updateOverallSummary(totalBudget: Double) {
        // This is partially handled inside the nested observer for now
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
            holder.binding.categorySpent.text = "R${"%.2f".format(spent)} / R${"%.2f".format(budget)}"
            
            val percent = if (budget > 0) (spent / budget * 100).toInt() else 0
            holder.binding.categoryProgress.progress = percent
            
            val left = budget - spent
            if (left >= 0) {
                holder.binding.categoryStatus.text = "R${"%.2f".format(left)} left"
                holder.binding.categoryStatus.setTextColor(resources.getColor(com.example.coincompass.R.color.mid_green, null))
                holder.binding.categoryProgress.setIndicatorColor(resources.getColor(com.example.coincompass.R.color.mid_green, null))
            } else {
                holder.binding.categoryStatus.text = "R${"%.2f".format(-left)} over budget"
                holder.binding.categoryStatus.setTextColor(resources.getColor(com.example.coincompass.R.color.delete_red, null))
                holder.binding.categoryProgress.setIndicatorColor(resources.getColor(com.example.coincompass.R.color.delete_red, null))
            }
        }

        override fun getItemCount() = categories.size

        inner class ViewHolder(val binding: ItemCategoryBudgetBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
