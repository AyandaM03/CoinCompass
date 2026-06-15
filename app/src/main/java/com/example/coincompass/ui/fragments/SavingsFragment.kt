package com.example.coincompass.ui.fragments

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.SavingsGoal
import com.example.coincompass.databinding.FragmentSavingsBinding
import com.example.coincompass.databinding.ItemSavingsGoalBinding
import com.example.coincompass.databinding.DialogAddSavingsGoalBinding
import com.example.coincompass.databinding.DialogAddFundsBinding
import com.example.coincompass.ui.AddSavingsGoalActivity
import com.example.coincompass.ui.BoostGoalActivity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SavingsFragment : Fragment() {

    private var _binding: FragmentSavingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var adapter: SavingsGoalAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSavingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())

        setupRecyclerView()
        observeData()

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.fabAddGoal.setOnClickListener {
            startActivity(Intent(requireContext(), AddSavingsGoalActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = SavingsGoalAdapter()
        binding.savingsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.savingsRecycler.adapter = adapter
    }

    private fun observeData() {
        db.savingsGoalDao().getAllSavingsGoals().observe(viewLifecycleOwner) { goals ->
            val nonNullGoals = goals ?: emptyList()
            if (nonNullGoals.isEmpty()) {
                binding.emptyState.root.visibility = View.VISIBLE
                binding.savingsRecycler.visibility = View.GONE
                updatePerformanceOverview(emptyList())
            } else {
                binding.emptyState.root.visibility = View.GONE
                binding.savingsRecycler.visibility = View.VISIBLE
                adapter.submitList(nonNullGoals)
                updatePerformanceOverview(nonNullGoals)
            }
        }
    }

    private fun updatePerformanceOverview(goals: List<SavingsGoal>) {
        if (_binding == null) return
        
        val totalGoals = goals.size
        val completedGoals = goals.count { it.currentAmount >= it.targetAmount }
        val actualTotalSaved = goals.sumOf { it.currentAmount }
        val totalTarget = goals.sumOf { it.targetAmount }
        
        // Progress calculation based on Requirements 2 & 4
        // We use capped saved amount to reflect actual progress towards completion of all goals
        val cappedSavedAmount = goals.sumOf { minOf(it.currentAmount, it.targetAmount) }
        val overallProgress = if (totalTarget > 0) ((cappedSavedAmount / totalTarget) * 100).toInt() else 0
        
        // Update circular gauge and percentage (Requirement 4)
        binding.performanceGauge.progress = overallProgress.coerceAtMost(100)
        binding.performancePercent.text = "$overallProgress%"
        
        // Update summary stats (Requirement 4)
        binding.totalGoalsText.text = "$totalGoals"
        binding.completedGoalsText.text = "$completedGoals"
        binding.totalSavedText.text = "R${"%.2f".format(actualTotalSaved)}"

        // Status Logic for Performance Overview (Requirement 3)
        // Red: 0-49%, Yellow: 50-79%, Green: 80-100%
        when {
            overallProgress < 50 -> {
                binding.performanceStatusText.text = "NEEDS EFFORT"
                val color = ContextCompat.getColor(requireContext(), R.color.expense_red)
                binding.performanceStatusText.setTextColor(color)
                binding.performanceGauge.setIndicatorColor(color)
            }
            overallProgress < 80 -> {
                binding.performanceStatusText.text = "MAKING PROGRESS"
                val color = ContextCompat.getColor(requireContext(), R.color.gold_accent)
                binding.performanceStatusText.setTextColor(color)
                binding.performanceGauge.setIndicatorColor(color)
            }
            else -> {
                binding.performanceStatusText.text = "EXCELLENT"
                val color = ContextCompat.getColor(requireContext(), R.color.primary_green)
                binding.performanceStatusText.setTextColor(color)
                binding.performanceGauge.setIndicatorColor(color)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class SavingsGoalAdapter : RecyclerView.Adapter<SavingsGoalAdapter.ViewHolder>() {
        private var list: List<SavingsGoal> = emptyList()

        fun submitList(newList: List<SavingsGoal>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemSavingsGoalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.binding.goalName.text = item.name
            holder.binding.goalDeadline.text = "Due: ${item.deadline}"
            holder.binding.savedAmount.text = "R${"%.0f".format(item.currentAmount)}"
            holder.binding.targetAmount.text = "R${"%.0f".format(item.targetAmount)}"
            holder.binding.goalEmoji.text = item.icon
            
            val progress = if (item.targetAmount > 0) (item.currentAmount / item.targetAmount * 100).toInt() else 0
            holder.binding.goalProgress.progress = progress.coerceAtMost(100)
            holder.binding.percentComplete.text = "$progress%"

            // Progress Color Logic (Consistent with Performance Overview)
            val context = holder.itemView.context
            val progressColor = when {
                progress < 50 -> ContextCompat.getColor(context, R.color.expense_red)
                progress < 80 -> ContextCompat.getColor(context, R.color.gold_accent)
                else -> ContextCompat.getColor(context, R.color.primary_green)
            }
            
            // Prioritize the logic-based status color for the progress visuals
            holder.binding.goalProgress.setIndicatorColor(progressColor)
            holder.binding.percentComplete.setTextColor(progressColor)

            holder.binding.btnDelete.setOnClickListener {
                lifecycleScope.launch {
                    db.savingsGoalDao().delete(item)
                }
            }

            holder.binding.btnEdit.setOnClickListener {
                val intent = Intent(requireContext(), AddSavingsGoalActivity::class.java)
                intent.putExtra("goal_id", item.id) // Support editing if needed
                startActivity(intent)
            }

            holder.binding.btnAddFunds.setOnClickListener {
                // Navigate to Boost Your Goal Page
                val intent = Intent(requireContext(), BoostGoalActivity::class.java)
                intent.putExtra("goal_id", item.id)
                startActivity(intent)
            }
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(val binding: ItemSavingsGoalBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
