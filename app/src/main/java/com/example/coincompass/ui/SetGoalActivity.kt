package com.example.coincompass.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Goal
import com.example.coincompass.databinding.ActivitySetGoalBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SetGoalActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetGoalBinding
    private lateinit var db: AppDatabase
    private var currentSpending = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        val calendar = Calendar.getInstance()
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val startDate = "$year-${month.toString().padStart(2, '0')}-01"
        val endDate = "$year-${month.toString().padStart(2, '0')}-31"

        db.expenseDao().getExpensesBetweenDates(startDate, endDate).observe(this) { expenses ->
            currentSpending = expenses?.sumOf { it.amount } ?: 0.0
            // Re-fetch goal to update visuals with spending
            db.goalDao().getGoalForMonth(currentMonth).value?.let { updateVisuals(it) }
        }

        db.goalDao().getGoalForMonth(currentMonth).observe(this) { goal ->
            if (goal != null) {
                updateVisuals(goal)
                binding.minGoalEdit.setText(goal.minGoal.toString())
                binding.maxGoalEdit.setText(goal.maxGoal.toString())
            }
        }

        binding.saveGoalButton.setOnClickListener {
            val minStr = binding.minGoalEdit.text.toString()
            val maxStr = binding.maxGoalEdit.text.toString()

            if (minStr.isNotEmpty() && maxStr.isNotEmpty()) {
                val min = minStr.toDoubleOrNull() ?: 0.0
                val max = maxStr.toDoubleOrNull() ?: 0.0

                if (min > max) {
                    Toast.makeText(this, "Minimum cannot be more than maximum!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    val newGoal = Goal(month = currentMonth, minGoal = min, maxGoal = max)
                    db.goalDao().insertOrUpdate(newGoal)
                    Toast.makeText(this@SetGoalActivity, "Goal updated successfully!", Toast.LENGTH_SHORT).show()
                    
                    binding.currentStrategyCard.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100).withEndAction {
                        binding.currentStrategyCard.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }.start()
                }
            } else {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun updateVisuals(goal: Goal?) {
        val minGoal = goal?.minGoal ?: 0.0
        val maxGoal = goal?.maxGoal ?: 0.0

        binding.minGoalDisplay.text = "R${"%.2f".format(minGoal)}"
        binding.maxGoalDisplay.text = "R${"%.2f".format(maxGoal)}"

        if (maxGoal > 0) {
            val progress = ((currentSpending / maxGoal) * 100).toInt()
            binding.rangeIndicator.progress = progress.coerceAtMost(100)
            
            when {
                currentSpending < minGoal -> {
                    binding.statusBadge.text = "BELOW GOAL"
                    binding.statusBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.cat_transport) // Using amber/yellow for below goal as requested/appropriate
                }
                currentSpending <= maxGoal -> {
                    binding.statusBadge.text = "WITHIN GOAL"
                    binding.statusBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_green)
                }
                else -> {
                    binding.statusBadge.text = "ABOVE LIMIT"
                    binding.statusBadge.backgroundTintList = ContextCompat.getColorStateList(this, R.color.expense_red)
                }
            }
            binding.statusBadge.visibility = View.VISIBLE
        } else {
            binding.rangeIndicator.progress = 0
            binding.statusBadge.visibility = View.GONE
        }
    }
}
