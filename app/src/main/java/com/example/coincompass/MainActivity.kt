package com.example.coincompass

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.CategorySummary
import com.example.coincompass.data.Expense
import com.example.coincompass.databinding.ActivityMainBinding
import com.example.coincompass.ui.AddCategoryActivity
import com.example.coincompass.ui.AddExpenseActivity
import com.example.coincompass.ui.SetGoalActivity
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var startDate = "2024-01-01"
    private var endDate = "2026-12-31"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)

        binding.btnAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        binding.btnAddCategory.setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }

        binding.btnSetGoal.setOnClickListener {
            startActivity(Intent(this, SetGoalActivity::class.java))
        }

        binding.btnStartDate.setOnClickListener {
            showDatePicker { date ->
                startDate = date
                binding.tvStartDate.text = startDate
                refreshData(db)
            }
        }

        binding.btnEndDate.setOnClickListener {
            showDatePicker { date ->
                endDate = date
                binding.tvEndDate.text = endDate
                refreshData(db)
            }
        }

        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvCategorySummary.layoutManager = LinearLayoutManager(this)

        refreshData(db)
        observeGoals(db)
    }

    private fun observeGoals(db: AppDatabase) {
        val currentMonth = String.format(Locale.US, "%d-%02d", 
            Calendar.getInstance().get(Calendar.YEAR), 
            Calendar.getInstance().get(Calendar.MONTH) + 1)
        
        db.goalDao().getGoalForMonth(currentMonth).observe(this) { goal ->
            if (goal != null) {
                db.expenseDao().getExpensesBetweenDates("$currentMonth-01", "$currentMonth-31").observe(this) { expenses ->
                    val totalSpent = expenses.sumOf { it.amount }
                    binding.tvGoalStatus.text = String.format(Locale.US, "Spent: R%.2f / Max: R%.2f", totalSpent, goal.maxGoal)
                    val progress = if (goal.maxGoal > 0) (totalSpent / goal.maxGoal * 100).toInt() else 0
                    binding.pbGoal.progress = progress
                }
            }
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val date = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, day)
            onDateSelected(date)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun refreshData(db: AppDatabase) {
        db.expenseDao().getExpensesBetweenDates(startDate, endDate).observe(this) { expenses ->
            binding.rvExpenses.adapter = ExpenseAdapter(expenses)
            val total = expenses.sumOf { it.amount }
            binding.totalBalanceText.text = String.format(Locale.US, "R%.2f", total)
        }

        db.expenseDao().getCategorySummaries(startDate, endDate).observe(this) { summaries ->
            binding.rvCategorySummary.adapter = CategorySummaryAdapter(summaries)
        }
    }

    class ExpenseAdapter(private val items: List<Expense>) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {
        class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tv = TextView(parent.context).apply {
                setPadding(16, 16, 16, 16)
            }
            return ViewHolder(tv)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.textView.text = "${item.date} - ${item.categoryName}: R${item.amount}\n${item.description}"
            
            holder.textView.setOnClickListener {
                if (item.photoPath != null) {
                    val context = holder.textView.context
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(android.net.Uri.parse(item.photoPath), "image/*")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.startActivity(intent)
                }
            }
        }
        override fun getItemCount() = items.size
    }

    class CategorySummaryAdapter(private val items: List<CategorySummary>) : RecyclerView.Adapter<CategorySummaryAdapter.ViewHolder>() {
        class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val tv = TextView(parent.context).apply {
                setPadding(16, 16, 16, 16)
            }
            return ViewHolder(tv)
        }
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.textView.text = "${item.categoryName}: R${item.totalAmount}"
        }
        override fun getItemCount() = items.size
    }
}
