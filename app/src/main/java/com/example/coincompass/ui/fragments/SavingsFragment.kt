package com.example.coincompass.ui.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import kotlinx.coroutines.launch

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
            if (goals == null || goals.isEmpty()) {
                binding.emptyState.root.visibility = View.VISIBLE
                binding.savingsRecycler.visibility = View.GONE
            } else {
                binding.emptyState.root.visibility = View.GONE
                binding.savingsRecycler.visibility = View.VISIBLE
                adapter.submitList(goals)
            }
        }
    }

    private fun showAddGoalDialog() {
        val dialogBinding = DialogAddSavingsGoalBinding.inflate(layoutInflater)
        
        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_CoinCompass)
        builder.setView(dialogBinding.root)
        val dialog = builder.create()
        dialog.show()

        dialogBinding.saveGoalButton.text = "Add Goal"
        dialogBinding.saveGoalButton.setOnClickListener {
            val name = dialogBinding.goalNameEdit.text.toString().trim()
            val target = dialogBinding.goalTargetEdit.text.toString().toDoubleOrNull() ?: 0.0
            val deadline = dialogBinding.goalDeadlineEdit.text.toString().trim()
            
            if (name.isNotEmpty() && target > 0) {
                lifecycleScope.launch {
                    db.savingsGoalDao().insert(SavingsGoal(name = name, targetAmount = target, deadline = deadline))
                    dialog.dismiss()
                }
            } else {
                Toast.makeText(requireContext(), "Please fill in all details", Toast.LENGTH_SHORT).show()
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
            
            val progress = if (item.targetAmount > 0) (item.currentAmount / item.targetAmount * 100).toInt() else 0
            holder.binding.goalProgress.progress = progress.coerceAtMost(100)
            holder.binding.percentComplete.text = "$progress%"

            // Completed vs In-Progress Styling
            if (progress >= 100) {
                holder.binding.goalProgress.setIndicatorColor(resources.getColor(R.color.secondary_gold, null))
                holder.binding.percentComplete.setTextColor(resources.getColor(R.color.secondary_gold, null))
                holder.binding.goalIconContainer.setCardBackgroundColor(resources.getColor(R.color.cream_surface, null))
                holder.binding.goalIcon.setColorFilter(resources.getColor(R.color.secondary_gold, null))
            } else {
                holder.binding.goalProgress.setIndicatorColor(resources.getColor(R.color.primary_green, null))
                holder.binding.percentComplete.setTextColor(resources.getColor(R.color.primary_green, null))
                holder.binding.goalIconContainer.setCardBackgroundColor(resources.getColor(R.color.light_green, null))
                holder.binding.goalIcon.setColorFilter(resources.getColor(R.color.primary_green, null))
            }

            holder.binding.btnDelete.setOnClickListener {
                lifecycleScope.launch {
                    db.savingsGoalDao().delete(item)
                }
            }

            holder.binding.btnEdit.setOnClickListener {
                showEditGoalDialog(item)
            }

            holder.binding.btnAddFunds.setOnClickListener {
                showAddFundsDialog(item)
            }
        }

        private fun showEditGoalDialog(goal: SavingsGoal) {
            val dialogBinding = DialogAddSavingsGoalBinding.inflate(layoutInflater)
            
            dialogBinding.goalNameEdit.setText(goal.name)
            dialogBinding.goalTargetEdit.setText(goal.targetAmount.toString())
            dialogBinding.goalDeadlineEdit.setText(goal.deadline)
            dialogBinding.saveGoalButton.text = "Update Goal"

            val builder = AlertDialog.Builder(requireContext(), R.style.Theme_CoinCompass)
            builder.setView(dialogBinding.root)
            val dialog = builder.create()
            dialog.show()

            dialogBinding.saveGoalButton.setOnClickListener {
                val name = dialogBinding.goalNameEdit.text.toString().trim()
                val target = dialogBinding.goalTargetEdit.text.toString().toDoubleOrNull() ?: 0.0
                val deadline = dialogBinding.goalDeadlineEdit.text.toString().trim()
                
                if (name.isNotEmpty() && target > 0) {
                    lifecycleScope.launch {
                        db.savingsGoalDao().update(goal.copy(name = name, targetAmount = target, deadline = deadline))
                        dialog.dismiss()
                    }
                }
            }
        }

        private fun showAddFundsDialog(goal: SavingsGoal) {
            val dialogBinding = DialogAddFundsBinding.inflate(layoutInflater)
            
            val builder = AlertDialog.Builder(requireContext(), R.style.Theme_CoinCompass)
            builder.setView(dialogBinding.root)
            
            // Add a "Confirm" button manually to the dialog
            builder.setPositiveButton("Add Funds") { _, _ ->
                val amount = dialogBinding.addFundsEdit.text.toString().toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    lifecycleScope.launch {
                        val updated = goal.copy(currentAmount = goal.currentAmount + amount)
                        db.savingsGoalDao().update(updated)
                    }
                }
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(val binding: ItemSavingsGoalBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
