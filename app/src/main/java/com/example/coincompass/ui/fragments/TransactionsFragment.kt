package com.example.coincompass.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Expense
import com.example.coincompass.databinding.FragmentTransactionsBinding
import com.example.coincompass.databinding.ItemExpenseBinding
import com.example.coincompass.ui.AddExpenseActivity
import com.example.coincompass.ui.ExpenseDetailActivity

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var adapter: TransactionAdapter
    
    private var allTransactions: List<Expense> = emptyList()
    private var currentFilter = "All"
    private var currentSearch = ""
    private var isSortDescending = true

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

        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_income -> "Income"
                R.id.chip_expense -> "Expense"
                R.id.chip_sort_date -> {
                    isSortDescending = !isSortDescending
                    applyFilters()
                    return@setOnCheckedStateChangeListener
                }
                else -> "All"
            }
            applyFilters()
        }
    }

    private fun observeData() {
        db.expenseDao().getAllExpenses().observe(viewLifecycleOwner) { expenses ->
            allTransactions = expenses
            applyFilters()
        }
    }

    private fun applyFilters() {
        var filtered = allTransactions

        // Type filter
        if (currentFilter != "All") {
            filtered = filtered.filter { it.type == currentFilter }
        }

        // Search filter
        if (currentSearch.isNotEmpty()) {
            filtered = filtered.filter { 
                it.description.contains(currentSearch, ignoreCase = true) || 
                it.categoryName.contains(currentSearch, ignoreCase = true) 
            }
        }

        // Sort
        filtered = if (isSortDescending) {
            filtered.sortedByDescending { it.date }
        } else {
            filtered.sortedBy { it.date }
        }

        adapter.submitList(filtered)
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
                holder.binding.expenseAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.mid_green))
                holder.binding.typeIcon.setImageResource(R.drawable.ic_add)
                holder.binding.typeIcon.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_green))
                holder.binding.typeIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.dark_green))
            } else {
                holder.binding.expenseAmount.text = "-R${"%.2f".format(item.amount)}"
                holder.binding.expenseAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.delete_red))
                holder.binding.typeIcon.setImageResource(R.drawable.ic_history)
                holder.binding.typeIcon.setBackgroundColor(Color.parseColor("#FFEBEE"))
                holder.binding.typeIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.delete_red))
            }

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

private fun android.widget.ImageView.setBackgroundColor(color: Int) {
    this.background.setTint(color)
}
