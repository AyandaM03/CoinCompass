package com.example.coincompass.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Goal
import com.example.coincompass.databinding.ActivitySetGoalBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// This class allows users to set their monthly spending goals
class SetGoalActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetGoalBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        // Get the current month (e.g., 2023-10)
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        // Look for an existing goal for this month
        db.goalDao().getGoalForMonth(currentMonth).observe(this) { goal ->
            if (goal != null) {
                binding.currentGoalText.text = "Min: R${"%.2f".format(goal.minGoal)}\nMax: R${"%.2f".format(goal.maxGoal)}"
                binding.minGoalEdit.setText(goal.minGoal.toString())
                binding.maxGoalEdit.setText(goal.maxGoal.toString())
            }
        }

        // When the user clicks "Save Goal"
        binding.saveGoalButton.setOnClickListener {
            val minStr = binding.minGoalEdit.text.toString()
            val maxStr = binding.maxGoalEdit.text.toString()

            if (minStr.isNotEmpty() && maxStr.isNotEmpty()) {
                val min = minStr.toDoubleOrNull() ?: 0.0
                val max = maxStr.toDoubleOrNull() ?: 0.0

                // Check if min is actually less than max
                if (min > max) {
                    Toast.makeText(this, "Minimum cannot be more than maximum!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Save to database in a background thread
                lifecycleScope.launch {
                    val newGoal = Goal(month = currentMonth, minGoal = min, maxGoal = max)
                    db.goalDao().insertOrUpdate(newGoal)
                    Toast.makeText(this@SetGoalActivity, "Goal saved for this month!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Back button to go home
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
