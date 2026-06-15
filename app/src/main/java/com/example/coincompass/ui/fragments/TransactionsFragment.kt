package com.example.coincompass.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Category
import com.example.coincompass.data.Expense
import com.example.coincompass.databinding.FragmentTransactionsBinding
import com.example.coincompass.databinding.ItemExpenseBinding
import com.example.coincompass.ui.AddExpenseActivity
import com.example.coincompass.ui.ExpenseDetailActivity
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment responsible for displaying a filterable list of all financial transactions.
 * Includes search functionality, categorical filtering, and date range selection.
 */
class TransactionsFragment : Fragment() {

    // ViewBinding instance using the recommended nullable pattern for Fragments
    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var adapter: TransactionAdapter
    
    // Data structures for holding source transactions and available categories
    private var allTransactions: List<Expense> = emptyList()
    private var categories: List<Category> = emptyList()
    
    // State variables for maintaining active filter criteria
    private var currentTypeFilter = "All"
    private var currentDateFilter = "All"
    private var customStartDate = ""
    private var customEndDate = ""
    private var currentSearch = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            db = AppDatabase.getDatabase(requireContext())
            setupRecyclerView()
            setupListeners()
            observeData()
            setupSwipeActions()
        } catch (e: Exception) {
            Log.e("TransactionsFragment", "Error in onViewCreated", e)
        }
    }

    // Configures the RecyclerView with a LinearLayoutManager and the custom adapter
    private fun setupRecyclerView() {
        adapter = TransactionAdapter()
        binding.transactionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionsRecycler.adapter = adapter
    }

    // Initialization of event listeners for interactive components (FAB, search bar, chip groups)
    private fun setupListeners() {
        binding.fabAddTransaction.setOnClickListener {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }

        // Real-time search filter update based on user input
        binding.searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearch = s.toString()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Filter transactions by type (Income vs Expense)
        binding.typeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentTypeFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_income -> "Income"
                R.id.chip_expense -> "Expense"
                else -> "All"
            }
            applyFilters()
        }

        // Filter transactions by predefined or custom date ranges
        binding.dateChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_today -> {
                    currentDateFilter = "Today"
                    applyFilters()
                }
                R.id.chip_month -> {
                    currentDateFilter = "Month"
                    applyFilters()
                }
                R.id.chip_range -> {
                    showRangePicker()
                }
                else -> {
                    currentDateFilter = "All"
                    applyFilters()
                }
            }
        }
    }

    // Displays the Material Design date range picker for custom temporal filtering
    private fun showRangePicker() {
        try {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Dates")
                .setSelection(Pair(MaterialDatePicker.todayInUtcMilliseconds(), MaterialDatePicker.todayInUtcMilliseconds()))
                .build()
                
            picker.addOnPositiveButtonClickListener { range ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                customStartDate = sdf.format(Date(range.first))
                customEndDate = sdf.format(Date(range.second))
                currentDateFilter = "Range"
                applyFilters()
            }
            picker.show(childFragmentManager, "TRANSACTION_RANGE")
        } catch (e: Exception) {
            Log.e("TransactionsFragment", "Error showing range picker", e)
        }
    }

    // Establishes LiveData observers for categories and expenses to enable reactive UI updates
    private fun observeData() {
        db.categoryDao().getAllCategories().observe(viewLifecycleOwner) { 
            categories = it ?: emptyList()
            if (::adapter.isInitialized) {
                adapter.notifyDataSetChanged()
            }
        }

        db.expenseDao().getAllExpenses().observe(viewLifecycleOwner) { expenses ->
            allTransactions = expenses ?: emptyList()
            applyFilters()
        }
    }

    // Consolidates active filter states and search queries to produce the final displayed transaction list
    private fun applyFilters() {
        if (_binding == null) return
        
        var filtered = allTransactions

        // Category/Type filter application
        if (currentTypeFilter != "All") {
            filtered = filtered.filter { it.type == currentTypeFilter }
        }

        // Date range filter application
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        when (currentDateFilter) {
            "Today" -> {
                val today = sdf.format(cal.time)
                filtered = filtered.filter { it.date == today }
            }
            "Month" -> {
                val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
                filtered = filtered.filter { it.date.startsWith(monthPrefix) }
            }
            "Range" -> {
                filtered = filtered.filter { it.date >= customStartDate && it.date <= customEndDate }
            }
        }

        // Text search filter application (checks description and category name)
        if (currentSearch.isNotEmpty()) {
            filtered = filtered.filter { 
                it.description.contains(currentSearch, ignoreCase = true) || 
                it.categoryName.contains(currentSearch, ignoreCase = true) 
            }
        }

        // Sort by date descending and update the adapter
        if (::adapter.isInitialized) {
            adapter.submitList(filtered.sortedByDescending { it.date })
        }
        
        // Handle visibility of the empty state UI component
        if (filtered.isEmpty()) {
            binding.emptyState.root.visibility = View.VISIBLE
            binding.transactionsRecycler.visibility = View.GONE
        } else {
            binding.emptyState.root.visibility = View.GONE
            binding.transactionsRecycler.visibility = View.VISIBLE
        }
    }

    // Configures swipe-to-edit (right) and swipe-to-delete (left) gestures using ItemTouchHelper
    private fun setupSwipeActions() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(r: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return
                
                val expense = adapter.getItemAt(position)
                
                if (direction == ItemTouchHelper.LEFT) {
                    // Execute deletion on a background thread
                    lifecycleScope.launch {
                        try {
                            db.expenseDao().delete(expense)
                            context?.let {
                                Toast.makeText(it, "Transaction deleted", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("TransactionsFragment", "Error deleting expense", e)
                        }
                    }
                } else {
                    // Launch modification activity
                    val intent = Intent(requireContext(), AddExpenseActivity::class.java)
                    intent.putExtra("expense_id", expense.id)
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

    /**
     * Inner class adapter for managing the list of transaction items.
     */
    inner class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {
        private var list: List<Expense> = emptyList()

        fun submitList(newList: List<Expense>) {
            list = newList
            notifyDataSetChanged()
        }

        fun getItemAt(position: Int): Expense = list[position]

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
                
                // Map category icons/emojis from the category reference list
                val category = categories.find { it.name == item.categoryName }
                holder.binding.categoryEmoji.text = category?.icon ?: "📁"
                
                // Dynamically update colors and indicators based on transaction type (Income vs Expense)
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

                // Handle conditional visibility and loading of receipt images
                if (!item.photoPath.isNullOrEmpty()) {
                    holder.binding.transactionImage.visibility = View.VISIBLE
                    if (item.photoPath == "camera_bitmap") {
                        holder.binding.transactionImage.setImageResource(android.R.drawable.ic_menu_camera)
                    } else {
                        try {
                            val uri = Uri.parse(item.photoPath)
                            // Verify stream access to prevent runtime crashes during onBind
                            context.contentResolver.openInputStream(uri)?.use { 
                                holder.binding.transactionImage.setImageURI(uri)
                            } ?: run {
                                holder.binding.transactionImage.visibility = View.GONE
                            }
                        } catch (e: Exception) {
                            Log.e("TransactionsFragment", "Error loading image: ${item.photoPath}", e)
                            holder.binding.transactionImage.visibility = View.GONE
                        }
                    }
                } else {
                    holder.binding.transactionImage.visibility = View.GONE
                }

                holder.itemView.setOnClickListener {
                    val intent = Intent(requireContext(), ExpenseDetailActivity::class.java)
                    intent.putExtra("expense_id", item.id)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e("TransactionsFragment", "Error binding view holder at pos $position", e)
            }
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
