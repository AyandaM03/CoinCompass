package com.example.coincompass.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
    private var selectedDateStr = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())

        setupRecyclerView()
        setupCalendar()
        observeData()

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = TransactionAdapter()
        binding.dayTransactionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.dayTransactionsRecycler.adapter = adapter
    }

    private fun setupCalendar() {
        val today = CalendarDay.today()
        binding.calendarView.selectedDate = today
        updateSelectedDateText(today)
        
        binding.calendarView.setOnDateChangedListener { _, date, _ ->
            updateSelectedDateText(date)
            filterTransactionsByDate(date)
        }
    }

    private fun updateSelectedDateText(date: CalendarDay) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.set(date.year, date.month - 1, date.day)
        selectedDateStr = sdf.format(cal.time)
        
        val displaySdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        binding.selectedDateText.text = displaySdf.format(cal.time)
    }

    private fun observeData() {
        db.expenseDao().getAllExpenses().observe(viewLifecycleOwner) { expenses ->
            allTransactions = expenses ?: emptyList()
            updateCalendarDecorators()
            val today = binding.calendarView.selectedDate
            if (today != null) filterTransactionsByDate(today)
        }
    }

    private fun updateCalendarDecorators() {
        val datesWithTransactions = allTransactions.map { 
            val parts = it.date.split("-")
            CalendarDay.from(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }.toSet()

        binding.calendarView.removeDecorators()
        binding.calendarView.addDecorator(EventDecorator(ContextCompat.getColor(requireContext(), R.color.mid_green), datesWithTransactions))
    }

    private fun filterTransactionsByDate(date: CalendarDay) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.set(date.year, date.month - 1, date.day)
        val dateStr = sdf.format(cal.time)

        val filtered = allTransactions.filter { it.date == dateStr }
        adapter.submitList(filtered)

        val total = filtered.sumOf { if (it.type == "Income") it.amount else -it.amount }
        binding.dailyTotalText.text = "R${"%.2f".format(total)}"
        binding.transactionCountText.text = "${filtered.size} items"
        
        if (total >= 0) {
            binding.dailyTotalText.setTextColor(Color.WHITE)
        } else {
            binding.dailyTotalText.setTextColor(ContextCompat.getColor(requireContext(), R.color.gold))
        }
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
                holder.binding.typeIcon.background.setTint(ContextCompat.getColor(requireContext(), R.color.light_green))
                holder.binding.typeIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.dark_green))
            } else {
                holder.binding.expenseAmount.text = "-R${"%.2f".format(item.amount)}"
                holder.binding.expenseAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.delete_red))
                holder.binding.typeIcon.setImageResource(R.drawable.ic_history)
                holder.binding.typeIcon.background.setTint(Color.parseColor("#FFEBEE"))
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

    class EventDecorator(private val color: Int, private val dates: Collection<CalendarDay>) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean = dates.contains(day)
        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(5f, color))
        }
    }
}
