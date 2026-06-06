package com.example.coincompass.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.CategorySummary
import com.example.coincompass.databinding.FragmentHomeBinding
import com.example.coincompass.databinding.ItemBudgetCategoryBinding
import com.example.coincompass.ui.AddCategoryActivity
import com.example.coincompass.ui.AddExpenseActivity
import com.example.coincompass.ui.HistoryActivity
import com.example.coincompass.ui.LoginActivity
import com.example.coincompass.ui.SetGoalActivity
import java.util.Calendar

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

        // Display the logged-in user's name
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val currentUserName = sharedPref.getString("current_username", "User")
        binding.userName.text = currentUserName

        // Set up the buttons to go to other screens
        binding.btnAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }

        binding.btnCategories.setOnClickListener {
            startActivity(Intent(requireContext(), AddCategoryActivity::class.java))
        }

        binding.btnGoals.setOnClickListener {
            startActivity(Intent(requireContext(), SetGoalActivity::class.java))
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(requireContext(), HistoryActivity::class.java))
        }

        // Logout button logic
        binding.btnLogout.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        // Set up the budget overview list
        val adapter = BudgetAdapter()
        binding.budgetRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.budgetRecycler.adapter = adapter

        // Get this month's expenses for the summary
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val startDate = "$year-${month.toString().padStart(2, '0')}-01"
        val endDate = "$year-${month.toString().padStart(2, '0')}-31"

        // Watch the database for changes and update the list
        db.expenseDao().getCategorySummaries(startDate, endDate).observe(viewLifecycleOwner) { summaries ->
            adapter.submitList(summaries)
            
            // Calculate total spent this month
            val totalSpent = summaries.sumOf { it.totalAmount }
            binding.expenseAmount.text = "R${"%.2f".format(totalSpent)}"
            
            // Income starts at 0 as requested previously
            val income = 0.00 
            binding.totalBalance.text = "R${"%.2f".format(income - totalSpent)}"
            binding.incomeAmount.text = "R${"%.2f".format(income)}"
        }

        // Connect the "Savings Summary" to the user's goals
        val currentMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
        db.goalDao().getGoalForMonth(currentMonth).observe(viewLifecycleOwner) { goal ->
            if (goal != null) {
                binding.savingsCard.visibility = View.VISIBLE
                binding.savingsGoalDisplay.text = "Min: R${"%.2f".format(goal.minGoal)} | Max: R${"%.2f".format(goal.maxGoal)}"
            } else {
                binding.savingsCard.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // List adapter for the Budget Overview
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
            holder.binding.categoryAmount.text = "R${"%.2f".format(item.totalAmount)}"
            
            // Set the progress bar
            holder.binding.categoryProgress.progress = (item.totalAmount / 5000 * 100).toInt()
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(val binding: ItemBudgetCategoryBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
