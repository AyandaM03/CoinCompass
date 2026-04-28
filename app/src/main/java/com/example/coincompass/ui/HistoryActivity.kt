package com.example.coincompass.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Expense
import com.example.coincompass.databinding.ActivityHistoryBinding
import com.example.coincompass.databinding.ItemExpenseBinding

// This class shows the user everything they have spent
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        // Set the list
        val adapter = HistoryAdapter()
        binding.historyRecycler.layoutManager = LinearLayoutManager(this)
        binding.historyRecycler.adapter = adapter

        // Get all expenses from database and show them
        db.expenseDao().getAllExpenses().observe(this) { expenses ->
            adapter.submitList(expenses)
        }

        // Back button to go home
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    // List adapter for the History
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
            val item = list[position]
            holder.binding.expenseCategory.text = item.categoryName
            holder.binding.expenseAmount.text = "R${"%.2f".format(item.amount)}"
            holder.binding.expenseDesc.text = item.description
            holder.binding.expenseDate.text = item.date

            // Click to see details
            holder.itemView.setOnClickListener {
                val intent = Intent(this@HistoryActivity, com.example.coincompass.ui.ExpenseDetailActivity::class.java)
                intent.putExtra("expense_id", item.id)
                startActivity(intent)
            }
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
