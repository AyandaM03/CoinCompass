package com.example.coincompass.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Expense
import com.example.coincompass.databinding.FragmentTransactionsBinding
import com.example.coincompass.databinding.ItemExpenseBinding
import com.example.coincompass.ui.AddExpenseActivity
import com.example.coincompass.ui.ExpenseDetailActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var adapter: TransactionAdapter
    
    private var allTransactions: List<Expense> = emptyList()
    private var currentTypeFilter = "All"
    private var currentDateFilter = "All" // All, Today, Week, Month
    private var currentSearch = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())

        setupRecyclerView()
        setupListeners()
        observeData()
        setupSwipeActions()
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter()
        binding.transactionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionsRecycler.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAddTransaction.setOnClickListener {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }

        binding.searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearch = s.toString()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.typeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentTypeFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_income -> "Income"
                R.id.chip_expense -> "Expense"
                else -> "All"
            }
            applyFilters()
        }

        binding.dateChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentDateFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_today -> "Today"
                R.id.chip_week -> "Week"
                R.id.chip_month -> "Month"
                else -> "All"
            }
            applyFilters()
        }
    }

    private fun observeData() {
        db.expenseDao().getAllExpenses().observe(viewLifecycleOwner) { expenses ->
            allTransactions = expenses ?: emptyList()
            updateSummary(allTransactions)
            applyFilters()
        }
    }

    private fun updateSummary(expenses: List<Expense>) {
        val income = expenses.filter { it.type == "Income" }.sumOf { it.amount }
        val spent = expenses.filter { it.type == "Expense" }.sumOf { it.amount }
        val balance = income - spent

        binding.summaryIncome.text = "+R${"%.2f".format(income)}"
        binding.summaryExpenses.text = "-R${"%.2f".format(spent)}"
        binding.summaryBalance.text = "R${"%.2f".format(balance)}"
        
        if (balance < 0) {
            binding.summaryBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red))
        } else {
            binding.summaryBalance.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold_accent))
        }
    }

    private fun applyFilters() {
        var filtered = allTransactions

        // Type filter
        if (currentTypeFilter != "All") {
            filtered = filtered.filter { it.type == currentTypeFilter }
        }

        // Date filter
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        when (currentDateFilter) {
            "Today" -> {
                val today = sdf.format(cal.time)
                filtered = filtered.filter { it.date == today }
            }
            "Week" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                val startOfWeek = sdf.format(cal.time)
                filtered = filtered.filter { it.date >= startOfWeek }
            }
            "Month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val startOfMonth = sdf.format(cal.time)
                filtered = filtered.filter { it.date >= startOfMonth }
            }
        }

        // Search filter
        if (currentSearch.isNotEmpty()) {
            filtered = filtered.filter { 
                it.description.contains(currentSearch, ignoreCase = true) || 
                it.categoryName.contains(currentSearch, ignoreCase = true) 
            }
        }

        adapter.submitList(filtered.sortedByDescending { it.date })
        
        if (filtered.isEmpty()) {
            binding.emptyState.root.visibility = View.VISIBLE
            binding.transactionsRecycler.visibility = View.GONE
        } else {
            binding.emptyState.root.visibility = View.GONE
            binding.transactionsRecycler.visibility = View.VISIBLE
        }
    }

    private fun setupSwipeActions() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(r: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val expense = adapter.getItemAt(position)
                
                if (direction == ItemTouchHelper.LEFT) {
                    // Delete
                    lifecycleScope.launch {
                        db.expenseDao().delete(expense)
                        Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Edit
                    val intent = Intent(requireContext(), AddExpenseActivity::class.java)
                    intent.putExtra("expense_id", expense.id) // Assuming AddExpense handles edit mode
                    startActivity(intent)
                    adapter.notifyItemChanged(position)
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.transactionsRecycler)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {
        private var list: List<Expense> = emptyList()

        fun submitList(newList: List<Expense>) {
            list = newList
            notifyDataSetChanged()
        }

        fun getItemAt(position: Int) = list[position]

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.binding.expenseCategory.text = item.categoryName
            holder.binding.expenseDesc.text = item.description
            holder.binding.expenseDate.text = item.date
            
            if (item.type == "Income") {
                holder.binding.expenseAmount.text = "+R${"%.2f".format(item.amount)}"
                holder.binding.expenseAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
                holder.binding.iconContainer.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.soft_mint))
                holder.binding.typeIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary_green))
            } else {
                holder.binding.expenseAmount.text = "-R${"%.2f".format(item.amount)}"
                holder.binding.expenseAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red))
                holder.binding.iconContainer.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                holder.binding.typeIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.expense_red))
            }

            holder.binding.imgIndicator.visibility = if (item.photoPath != null) View.VISIBLE else View.GONE

            holder.itemView.setOnClickListener {
                val intent = Intent(requireContext(), ExpenseDetailActivity::class.java)
                intent.putExtra("expense_id", item.id)
                startActivity(intent)
            }
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
