package com.example.coincompass

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.CategorySummary
import com.example.coincompass.databinding.ActivityMainBinding
import com.example.coincompass.databinding.ItemBudgetCategoryBinding
import com.example.coincompass.ui.AddCategoryActivity
import com.example.coincompass.ui.AddExpenseActivity
import java.util.Calendar

// This is the home screen of our app
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        // Setup the buttons to go to other screens
        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        binding.btnCategories.setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, com.example.coincompass.ui.HistoryActivity::class.java))
        }

        // Setup the budget overview list
        val adapter = BudgetAdapter()
        binding.budgetRecycler.layoutManager = LinearLayoutManager(this)
        binding.budgetRecycler.adapter = adapter

        // Get this month's expenses for the summary
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val startDate = "$year-${month.toString().padStart(2, '0')}-01"
        val endDate = "$year-${month.toString().padStart(2, '0')}-31"

        // Watch the database for changes and update the list
        db.expenseDao().getCategorySummaries(startDate, endDate).observe(this) { summaries ->
            adapter.submitList(summaries)
            
            // Calculate total spent this month
            val totalSpent = summaries.sumOf { it.totalAmount }
            binding.expenseAmount.text = "R${"%.2f".format(totalSpent)}"
            
            // Just some fake balance math for now
            val income = 25000.00 
            binding.totalBalance.text = "R${"%.2f".format(income - totalSpent)}"
        }
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
            
            // Set the progress bar (e.g. 50% for now)
            holder.binding.categoryProgress.progress = (item.totalAmount / 5000 * 100).toInt()
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(val binding: ItemBudgetCategoryBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
