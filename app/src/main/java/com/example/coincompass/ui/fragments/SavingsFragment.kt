package com.example.coincompass.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
            showAddGoalDialog()
        }
    }

    private fun setupRecyclerView() {
        adapter = SavingsGoalAdapter()
        binding.savingsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.savingsRecycler.adapter = adapter
    }

    private fun observeData() {
        db.savingsGoalDao().getAllSavingsGoals().observe(viewLifecycleOwner) { goals ->
            adapter.submitList(goals)
        }
    }

    private fun showAddGoalDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_savings_goal, null)
        val nameEdit = dialogView.findViewById<EditText>(R.id.goal_name_edit)
        val targetEdit = dialogView.findViewById<EditText>(R.id.goal_target_edit)
        val deadlineEdit = dialogView.findViewById<EditText>(R.id.goal_deadline_edit)

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("New Savings Goal")
        builder.setView(dialogView)
        builder.setPositiveButton("Add") { _, _ ->
            val name = nameEdit.text.toString()
            val target = targetEdit.text.toString().toDoubleOrNull() ?: 0.0
            val deadline = deadlineEdit.text.toString()
            
            if (name.isNotEmpty() && target > 0) {
                lifecycleScope.launch {
                    db.savingsGoalDao().insert(SavingsGoal(name = name, targetAmount = target, deadline = deadline))
                }
            } else {
                Toast.makeText(requireContext(), "Please fill details", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
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
            holder.binding.goalDeadline.text = "Target: ${item.deadline}"
            holder.binding.savedAmount.text = "R${"%.2f".format(item.currentAmount)}"
            holder.binding.targetAmount.text = "of R${"%.2f".format(item.targetAmount)}"
            
            val progress = if (item.targetAmount > 0) (item.currentAmount / item.targetAmount * 100).toInt() else 0
            holder.binding.goalProgress.progress = progress

            // Achievement indicator
            if (progress >= 90) {
                holder.binding.achievementBadge.visibility = View.VISIBLE
                holder.binding.goalStatusIndicator.visibility = View.VISIBLE
                holder.binding.goalStatusIndicator.text = if (progress >= 100) "Goal Achieved!" else "Almost there!"
                holder.binding.goalStatusIndicator.setTextColor(resources.getColor(R.color.mid_green, null))
            } else {
                holder.binding.achievementBadge.visibility = View.GONE
                holder.binding.goalStatusIndicator.visibility = View.GONE
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
            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_savings_goal, null)
            val nameEdit = dialogView.findViewById<EditText>(R.id.goal_name_edit)
            val targetEdit = dialogView.findViewById<EditText>(R.id.goal_target_edit)
            val deadlineEdit = dialogView.findViewById<EditText>(R.id.goal_deadline_edit)

            nameEdit.setText(goal.name)
            targetEdit.setText(goal.targetAmount.toString())
            deadlineEdit.setText(goal.deadline)

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Edit Savings Goal")
            builder.setView(dialogView)
            builder.setPositiveButton("Save") { _, _ ->
                val name = nameEdit.text.toString()
                val target = targetEdit.text.toString().toDoubleOrNull() ?: 0.0
                val deadline = deadlineEdit.text.toString()
                
                if (name.isNotEmpty() && target > 0) {
                    lifecycleScope.launch {
                        db.savingsGoalDao().update(goal.copy(name = name, targetAmount = target, deadline = deadline))
                    }
                }
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()
        }

        private fun showAddFundsDialog(goal: SavingsGoal) {
            val input = EditText(requireContext())
            input.hint = "Amount to add (R)"
            input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.setTextColor(resources.getColor(R.color.black, null))
            input.setPadding(48, 32, 48, 32)

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Add Funds to ${goal.name}")
            builder.setView(input)
            builder.setPositiveButton("Add") { _, _ ->
                val amount = input.text.toString().toDoubleOrNull() ?: 0.0
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
