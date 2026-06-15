package com.example.coincompass.ui

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Category
import com.example.coincompass.data.CategorySummary
import com.example.coincompass.databinding.ActivityAddCategoryBinding
import com.example.coincompass.databinding.DialogAddCategoryBinding
import com.example.coincompass.databinding.ItemCategoryBudgetBinding
import kotlinx.coroutines.launch
import java.util.*

class AddCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCategoryBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: CategoryAdapter
    private var allCategories: List<Category> = emptyList()
    private var categorySummaries: List<CategorySummary> = emptyList()

    private val emojis = listOf(
        "🍔", "🍕", "🍟", "🍎", "☕", // Food
        "🚗", "🚌", "🚕", "🚆", // Transport
        "💊", "🏥", "❤️", "🧘", // Health
        "🛍️", "👕", "👟", // Shopping
        "🎮", "🎬", "🎵", "🎤", // Entertainment
        "📚", "🎓", "✏️", // Education
        "💰", "🏦", // Savings
        "🏠", "⚡", "📶", "🛒", "🛠️", "💅", "✈️" // General
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        adapter = CategoryAdapter()
        binding.categoriesRecycler.layoutManager = LinearLayoutManager(this)
        binding.categoriesRecycler.adapter = adapter

        observeData()
        setupListeners()
    }

    private fun observeData() {
        db.categoryDao().getAllCategories().observe(this) { categories ->
            allCategories = categories
            updateSummary()
            filterCategories(binding.searchEdit.text.toString())
        }

        db.expenseDao().getCategorySummaries("1970-01-01", "2100-12-31").observe(this) { summaries ->
            categorySummaries = summaries
            updateSummary()
            adapter.notifyDataSetChanged()
        }
    }

    private fun updateSummary() {
        binding.totalCategoriesText.text = allCategories.size.toString()
        val totalBudget = allCategories.sumOf { it.budgetAmount }
        binding.totalBudgetText.text = "R${"%.2f".format(totalBudget)}"
        
        val mostUsed = categorySummaries.maxByOrNull { it.totalAmount }
        binding.mostUsedCategoryText.text = mostUsed?.categoryName ?: "--"
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.addCategoryFab.setOnClickListener {
            showAddCategoryDialog()
        }

        binding.searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCategories(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterCategories(query: String) {
        val filtered = if (query.isEmpty()) {
            allCategories
        } else {
            allCategories.filter { it.name.contains(query, ignoreCase = true) }
        }
        adapter.submitList(filtered)
    }

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(this, R.style.Theme_CoinCompass)
        builder.setView(dialogBinding.root)
        val dialog = builder.create()
        dialog.show()

        var selectedEmoji = "📁"

        dialogBinding.emojiRecycler.layoutManager = GridLayoutManager(this, 5)
        dialogBinding.emojiRecycler.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(parent.context).apply {
                    textSize = 24f
                    gravity = android.view.Gravity.CENTER
                    setPadding(16, 16, 16, 16)
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
                return object : RecyclerView.ViewHolder(tv) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val emoji = emojis[position]
                (holder.itemView as TextView).text = emoji
                holder.itemView.setOnClickListener {
                    selectedEmoji = emoji
                    dialogBinding.previewEmoji.text = emoji
                }
            }

            override fun getItemCount() = emojis.size
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val name = dialogBinding.nameEdit.text.toString().trim()
                dialogBinding.previewName.text = if (name.isEmpty()) "Category Name" else name
                
                val budget = dialogBinding.budgetEdit.text.toString().toDoubleOrNull() ?: 0.0
                dialogBinding.previewBudget.text = "Monthly Budget: R${"%.2f".format(budget)}"
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        dialogBinding.nameEdit.addTextChangedListener(watcher)
        dialogBinding.budgetEdit.addTextChangedListener(watcher)

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.nameEdit.text.toString().trim()
            val budget = dialogBinding.budgetEdit.text.toString().toDoubleOrNull() ?: 0.0
            
            if (name.isNotEmpty()) {
                lifecycleScope.launch {
                    db.categoryDao().insert(Category(name = name, budgetAmount = budget, icon = selectedEmoji))
                    dialog.dismiss()
                    Toast.makeText(this@AddCategoryActivity, "Category created successfully!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class CategoryAdapter : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
        private var list: List<Category> = emptyList()

        fun submitList(newList: List<Category>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemCategoryBudgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            val summary = categorySummaries.find { it.categoryName == item.name }
            val spent = summary?.totalAmount ?: 0.0
            val budget = item.budgetAmount

            holder.binding.categoryName.text = item.name
            holder.binding.categoryEmoji.text = item.icon
            holder.binding.categorySpent.text = "R${"%.2f".format(spent)}"
            holder.binding.categoryBudget.text = "R${"%.2f".format(budget)}"
            
            val percent = if (budget > 0) (spent / budget * 100).toInt() else 0
            holder.binding.categoryProgress.progress = percent.coerceAtMost(100)
            holder.binding.percentLabel.text = "$percent%"
            
            val remaining = budget - spent
            holder.binding.categoryStatus.text = "R${"%.2f".format(remaining.coerceAtLeast(0.0))}"
            
            val statusColor = when {
                percent < 80 -> ContextCompat.getColor(this@AddCategoryActivity, R.color.primary_green)
                percent < 100 -> Color.parseColor("#FBBF24") // Amber
                else -> ContextCompat.getColor(this@AddCategoryActivity, R.color.expense_red)
            }
            
            holder.binding.categoryProgress.setIndicatorColor(statusColor)
            holder.binding.categoryStatus.setTextColor(statusColor)
            holder.binding.percentLabel.setTextColor(statusColor)

            // Category specific accent color for the icon background
            val accentColor = when (item.name.lowercase()) {
                "food", "groceries" -> Color.parseColor("#FFF3E0") // Light Orange
                "health", "medical" -> Color.parseColor("#FFEBEE") // Light Red
                "savings", "income" -> Color.parseColor("#E8F5E9") // Light Green
                "entertainment" -> Color.parseColor("#F3E5F5") // Light Purple
                "transport" -> Color.parseColor("#E3F2FD") // Light Blue
                else -> ContextCompat.getColor(this@AddCategoryActivity, R.color.soft_mint)
            }
            holder.binding.iconBg.setCardBackgroundColor(accentColor)
        }

        override fun getItemCount() = list.size
        inner class ViewHolder(val binding: ItemCategoryBudgetBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
