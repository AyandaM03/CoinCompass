package com.example.coincompass.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Category
import com.example.coincompass.data.Expense
import com.example.coincompass.databinding.ActivityHistoryBinding
import com.example.coincompass.databinding.ItemExpenseBinding

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var db: AppDatabase
    private var categories: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityHistoryBinding.inflate(layoutInflater)
            setContentView(binding.root)

            db = AppDatabase.getDatabase(this)

            val adapter = HistoryAdapter()
            binding.historyRecycler.layoutManager = LinearLayoutManager(this)
            binding.historyRecycler.adapter = adapter

            db.categoryDao().getAllCategories().observe(this) { 
                categories = it ?: emptyList()
                adapter.notifyDataSetChanged()
            }

            db.expenseDao().getAllExpenses().observe(this) { expenses ->
                adapter.submitList(expenses ?: emptyList())
            }

            binding.btnBack.setOnClickListener {
                finish()
            }
        } catch (e: Exception) {
            Log.e("HistoryActivity", "Error in onCreate", e)
        }
    }

    inner class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        private var list: List<Expense> = emptyList()

        @SuppressLint("NotifyDataSetChanged")
        fun submitList(newList: List<Expense>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            try {
                val item = list[position]
                val context = holder.itemView.context
                
                holder.binding.expenseCategory.text = item.categoryName
                holder.binding.expenseDesc.text = item.description
                holder.binding.expenseDate.text = item.date
                
                val category = categories.find { it.name == item.categoryName }
                holder.binding.categoryEmoji.text = category?.icon ?: "📁"
                
                if (item.type == "Income") {
                    holder.binding.expenseAmount.text = "+R${"%.2f".format(item.amount)}"
                    holder.binding.expenseAmount.setTextColor(ContextCompat.getColor(context, R.color.primary_green))
                    holder.binding.transactionCard.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                    holder.binding.iconContainer.setCardBackgroundColor(Color.WHITE)
                } else {
                    holder.binding.expenseAmount.text = "-R${"%.2f".format(item.amount)}"
                    holder.binding.expenseAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
                    holder.binding.transactionCard.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                    holder.binding.iconContainer.setCardBackgroundColor(Color.WHITE)
                }

                if (!item.photoPath.isNullOrEmpty()) {
                    holder.binding.transactionImage.visibility = View.VISIBLE
                    if (item.photoPath == "camera_bitmap") {
                        holder.binding.transactionImage.setImageResource(android.R.drawable.ic_menu_camera)
                    } else {
                        try {
                            holder.binding.transactionImage.setImageURI(Uri.parse(item.photoPath))
                        } catch (e: Exception) {
                            Log.e("HistoryActivity", "Error loading image", e)
                            holder.binding.transactionImage.visibility = View.GONE
                        }
                    }
                } else {
                    holder.binding.transactionImage.visibility = View.GONE
                }

                holder.itemView.setOnClickListener {
                    val intent = Intent(this@HistoryActivity, com.example.coincompass.ui.ExpenseDetailActivity::class.java)
                    intent.putExtra("expense_id", item.id)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e("HistoryActivity", "Error binding view holder", e)
            }
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
