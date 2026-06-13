package com.example.coincompass.ui.fragments

import android.content.Intent
import android.graphics.Color
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
import com.example.coincompass.databinding.FragmentCalendarBinding
import com.example.coincompass.databinding.ItemExpenseBinding
import com.example.coincompass.ui.AddExpenseActivity
import com.example.coincompass.ui.ExpenseDetailActivity
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var adapter: TransactionAdapter
    
    private var allTransactions: List<Expense> = emptyList()
    private var currentSearchQuery = ""
    private var currentTypeFilter = "All"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())

        setupRecyclerView()
        setupCalendar()
        setupListeners()
        observeData()

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter()
        binding.historyRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecycler.adapter = adapter
    }

    private fun setupCalendar() {
        binding.calendarView.setOnDateChangedListener { _, date, _ ->
            // Filter list by selected date if needed, or just highlight
        }
    }

    private fun setupListeners() {
        binding.searchEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString().lowercase()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentTypeFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_income -> "Income"
                R.id.chip_expense -> "Expense"
                else -> "All"
            }
            applyFilters()
        }
    }

    private fun observeData() {
        db.expenseDao().getAllExpenses().observe(viewLifecycleOwner) { expenses ->
            allTransactions = expenses ?: emptyList()
            updateCalendarDecorators()
            applyFilters()
        }
    }

    private fun updateCalendarDecorators() {
        val incomeDates = allTransactions.filter { it.type == "Income" }.map { parseDate(it.date) }.toSet()
        val expenseDates = allTransactions.filter { it.type == "Expense" }.map { parseDate(it.date) }.toSet()

        binding.calendarView.removeDecorators()
        binding.calendarView.addDecorator(EventDecorator(ContextCompat.getColor(requireContext(), R.color.primary_green), incomeDates))
        binding.calendarView.addDecorator(EventDecorator(ContextCompat.getColor(requireContext(), R.color.cat_food), expenseDates))
    }

    private fun parseDate(dateStr: String): CalendarDay {
        val parts = dateStr.split("-")
        return CalendarDay.from(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
    }

    private fun applyFilters() {
        val filtered = allTransactions.filter {
            val matchesSearch = it.description.lowercase().contains(currentSearchQuery) || 
                              it.categoryName.lowercase().contains(currentSearchQuery)
            val matchesType = currentTypeFilter == "All" || it.type == currentTypeFilter
            matchesSearch && matchesType
        }.sortedByDescending { it.date }
        
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
                holder.binding.expenseAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
                holder.binding.typeIcon.setImageResource(R.drawable.ic_add)
                holder.binding.typeIcon.background.setTint(ContextCompat.getColor(requireContext(), R.color.light_green))
                holder.binding.typeIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary_green))
            } else {
                holder.binding.expenseAmount.text = "-R${"%.2f".format(item.amount)}"
                holder.binding.expenseAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.cat_food))
                holder.binding.typeIcon.setImageResource(R.drawable.ic_history)
                holder.binding.typeIcon.background.setTint(Color.parseColor("#FFEBEE"))
                holder.binding.typeIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.cat_food))
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

    class EventDecorator(private val color: Int, private val dates: Collection<CalendarDay>) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean = dates.contains(day)
        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(8f, color))
        }
    }
}
