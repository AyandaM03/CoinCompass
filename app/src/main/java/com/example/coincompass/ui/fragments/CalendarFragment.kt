package com.example.coincompass.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Category
import com.example.coincompass.data.Expense
import com.example.coincompass.databinding.FragmentCalendarBinding
import com.example.coincompass.databinding.ItemExpenseBinding
import com.example.coincompass.ui.AddExpenseActivity
import com.example.coincompass.ui.ExpenseDetailActivity
import com.google.android.material.datepicker.MaterialDatePicker
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
    private var categories: List<Category> = emptyList()
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    private var currentStartDate: String = ""
    private var currentEndDate: String = ""
    private var selectedDay: CalendarDay = CalendarDay.today()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            db = AppDatabase.getDatabase(requireContext())

            binding.btnBack.setOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }

            setupRecyclerView()
            setupCalendar()
            setupListeners()
            
            updateRangeBasedOnCurrentSelection()
            observeData()

            // Page Entry Animation
            binding.calendarCard.translationY = 40f
            binding.calendarCard.alpha = 0f
            binding.calendarCard.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500)
                .start()
        } catch (e: Exception) {
            Log.e("CalendarFragment", "Error in onViewCreated", e)
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter()
        binding.historyRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecycler.adapter = adapter
    }

    private fun setupCalendar() {
        binding.calendarView.selectedDate = selectedDay
        binding.calendarView.setOnDateChangedListener { _, date, _ ->
            selectedDay = date
            updateRangeBasedOnCurrentSelection()
            applyFilters()
            
            binding.historyRecycler.alpha = 0f
            binding.historyRecycler.animate().alpha(1f).setDuration(300).start()
        }
    }

    private fun setupListeners() {
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            updateRangeBasedOnCurrentSelection()
            if (checkedIds.firstOrNull() == R.id.chip_range) {
                showCustomRangePicker()
            } else {
                applyFilters()
            }
        }

        binding.btnViewAnalytics.setOnClickListener {
            val bundle = Bundle().apply {
                putString("startDate", currentStartDate)
                putString("endDate", currentEndDate)
            }
            findNavController().navigate(R.id.nav_analytics, bundle)
        }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }
    }

    private fun updateRangeBasedOnCurrentSelection() {
        when (binding.filterChipGroup.checkedChipId) {
            R.id.chip_day -> updateRangeToSelectedDay()
            R.id.chip_week -> updateRangeToCurrentWeek()
            R.id.chip_month -> updateRangeToCurrentMonth()
            // chip_range is handled via date picker
        }
    }

    private fun updateRangeToSelectedDay() {
        val cal = Calendar.getInstance()
        cal.set(selectedDay.year, selectedDay.month - 1, selectedDay.day)
        currentStartDate = sdf.format(cal.time)
        currentEndDate = currentStartDate
        binding.periodLabel.text = "Selected Day"
    }

    private fun updateRangeToCurrentWeek() {
        val cal = Calendar.getInstance()
        cal.set(selectedDay.year, selectedDay.month - 1, selectedDay.day)
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        currentStartDate = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_WEEK, 6)
        currentEndDate = sdf.format(cal.time)
        binding.periodLabel.text = "This Week"
    }

    private fun updateRangeToCurrentMonth() {
        val cal = Calendar.getInstance()
        cal.set(selectedDay.year, selectedDay.month - 1, selectedDay.day)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        currentStartDate = sdf.format(cal.time)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        currentEndDate = sdf.format(cal.time)
        binding.periodLabel.text = "This Month"
    }

    private fun showCustomRangePicker() {
        try {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Dates")
                .setSelection(Pair(MaterialDatePicker.todayInUtcMilliseconds(), MaterialDatePicker.todayInUtcMilliseconds()))
                .build()
                
            picker.addOnPositiveButtonClickListener { range ->
                currentStartDate = sdf.format(Date(range.first))
                currentEndDate = sdf.format(Date(range.second))
                binding.periodLabel.text = "Custom Range"
                applyFilters()
            }
            picker.show(childFragmentManager, "RANGE_PICKER")
        } catch (e: Exception) {
            Log.e("CalendarFragment", "Error showing date picker", e)
        }
    }

    private fun observeData() {
        db.categoryDao().getAllCategories().observe(viewLifecycleOwner) { 
            categories = it ?: emptyList()
            if (::adapter.isInitialized) {
                adapter.notifyDataSetChanged()
            }
        }

        db.expenseDao().getAllExpenses().observe(viewLifecycleOwner) { expenses ->
            if (_binding == null || !isAdded) return@observe
            allTransactions = expenses ?: emptyList()
            updateCalendarDecorators()
            applyFilters()
        }
    }

    private fun updateCalendarDecorators() {
        if (_binding == null || !isAdded) return

        try {
            val incomeDates = allTransactions.filter { it.type == "Income" }
                .mapNotNull { parseDateToCalendarDay(it.date) }.toSet()
            
            val expenseDates = allTransactions.filter { it.type == "Expense" && !it.categoryName.lowercase().contains("savings") && !it.categoryName.lowercase().contains("goal") }
                .mapNotNull { parseDateToCalendarDay(it.date) }.toSet()

            val goalDates = allTransactions.filter { it.categoryName.lowercase().contains("savings") || it.categoryName.lowercase().contains("goal") }
                .mapNotNull { parseDateToCalendarDay(it.date) }.toSet()

            binding.calendarView.removeDecorators()
            
            binding.calendarView.addDecorator(EventDecorator(ContextCompat.getColor(requireContext(), R.color.primary_green), incomeDates))
            binding.calendarView.addDecorator(EventDecorator(ContextCompat.getColor(requireContext(), R.color.expense_red), expenseDates))
            binding.calendarView.addDecorator(EventDecorator(ContextCompat.getColor(requireContext(), R.color.gold_accent), goalDates))
        } catch (e: Exception) {
            Log.e("CalendarFragment", "Error updating decorators", e)
        }
    }

    private fun parseDateToCalendarDay(dateStr: String): CalendarDay? {
        return try {
            val parts = dateStr.split("-")
            CalendarDay.from(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        } catch (e: Exception) { null }
    }

    private fun applyFilters() {
        if (_binding == null) return
        
        val filtered = allTransactions.filter { 
            it.date >= currentStartDate && it.date <= currentEndDate 
        }.sortedByDescending { it.date }
        
        if (::adapter.isInitialized) {
            adapter.submitList(filtered)
        }
        
        val totalSpent = filtered.filter { it.type == "Expense" }.sumOf { it.amount }
        binding.totalSpentText.text = "R${"%.2f".format(totalSpent)}"
        binding.transactionCountText.text = "${filtered.size} items"
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
                            val uri = Uri.parse(item.photoPath)
                            // Verify permission by attempting to open stream, avoiding deferred crash in onMeasure
                            context.contentResolver.openInputStream(uri)?.use { 
                                holder.binding.transactionImage.setImageURI(uri)
                            } ?: run {
                                holder.binding.transactionImage.visibility = View.GONE
                            }
                        } catch (e: Exception) {
                            Log.e("CalendarFragment", "Error loading image", e)
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
                Log.e("CalendarFragment", "Error binding view holder", e)
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
