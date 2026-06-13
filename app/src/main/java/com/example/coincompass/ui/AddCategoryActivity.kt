package com.example.coincompass.ui

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Category
import com.example.coincompass.data.CategorySummary
import com.example.coincompass.databinding.ActivityAddCategoryBinding
import com.example.coincompass.databinding.DialogAddCategoryBinding
import com.example.coincompass.databinding.ItemCategoryBinding
import kotlinx.coroutines.launch
import java.util.*

class AddCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCategoryBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: CategoryAdapter
    private var allCategories: List<Category> = emptyList()
    private var categorySummaries: List<CategorySummary> = emptyList()

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
            filterCategories(binding.searchEdit.text.toString())
        }

        db.expenseDao().getCategorySummaries("1970-01-01", "2100-12-31").observe(this) { summaries ->
            categorySummaries = summaries
            adapter.notifyDataSetChanged()
        }
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

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.nameEdit.text.toString().trim()
            if (name.isNotEmpty()) {
                lifecycleScope.launch {
                    db.categoryDao().insert(Category(name = name, budgetAmount = 0.0))
                    dialog.dismiss()
                    Toast.makeText(this@AddCategoryActivity, "Category added", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Enter a name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCategoryColor(name: String): Int {
        return when (name.lowercase()) {
            "health", "healthcare" -> Color.parseColor("#00897B") // Teal
            "food", "food & dining" -> Color.parseColor("#F87171") // Coral/Red
            "transport" -> Color.parseColor("#FBBF24") // Amber
            "education" -> Color.parseColor("#2196F3") // Blue
            "entertainment" -> Color.parseColor("#8B5CF6") // Purple
            "groceries" -> Color.parseColor("#2E7D32") // Emerald Green
            "housing" -> Color.parseColor("#795548") // Brown
            "salary" -> Color.parseColor("#4CAF50") // Green
            "utilities" -> Color.parseColor("#FFCA28") // Amber
            "fitness" -> Color.parseColor("#EC407A") // Pink
            "shopping" -> Color.parseColor("#AB47BC") // Purple
            "investments" -> Color.parseColor("#26A69A") // Teal
            "travel" -> Color.parseColor("#26C6DA") // Cyan
            else -> {
                val colors = listOf("#4DB6AC", "#9575CD", "#F06292", "#4FC3F7", "#AED581", "#FF8A65")
                Color.parseColor(colors[Math.abs(name.hashCode()) % colors.size])
            }
        }
    }

    private fun getCategoryIcon(name: String): Int {
        return when (name.lowercase()) {
            "health", "healthcare" -> android.R.drawable.ic_menu_myplaces
            "food", "food & dining" -> R.drawable.ic_categories
            "transport" -> R.drawable.ic_calendar
            "education" -> R.drawable.ic_goals
            "entertainment" -> R.drawable.ic_analytics
            "groceries" -> R.drawable.ic_categories
            "salary", "freelance" -> R.drawable.ic_add
            "travel" -> R.drawable.ic_calendar
            "utilities" -> R.drawable.ic_categories
            else -> R.drawable.ic_categories
        }
    }

    inner class CategoryAdapter : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
        private var list: List<Category> = emptyList()

        fun submitList(newList: List<Category>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.binding.categoryName.text = item.name
            
            val summary = categorySummaries.find { it.categoryName == item.name }
            val amount = summary?.totalAmount ?: 0.0
            
            // Transaction count placeholder as requested in prompt
            holder.binding.categoryDescription.text = "0 Transactions" 
            
            holder.binding.categoryAmount.text = if (amount >= 0) "R${"%.2f".format(amount)}" else "-R${"%.2f".format(-amount)}"
            holder.binding.categoryAmount.setTextColor(if (amount >= 0) ContextCompat.getColor(this@AddCategoryActivity, R.color.income_green) else ContextCompat.getColor(this@AddCategoryActivity, R.color.expense_red))

            val color = getCategoryColor(item.name)
            holder.binding.iconContainer.setCardBackgroundColor(color)
            holder.binding.categoryIcon.setImageResource(getCategoryIcon(item.name))
        }

        override fun getItemCount() = list.size
        inner class ViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
