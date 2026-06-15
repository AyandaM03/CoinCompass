package com.example.coincompass.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.SavingsGoal
import com.example.coincompass.databinding.ActivityBoostGoalBinding
import kotlinx.coroutines.launch

class BoostGoalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBoostGoalBinding
    private lateinit var db: AppDatabase
    private var goalId: Long = -1
    private var currentGoal: SavingsGoal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoostGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        goalId = intent.getLongExtra("goal_id", -1)

        if (goalId == -1L) {
            finish()
            return
        }

        loadGoalData()
        setupListeners()
    }

    private fun loadGoalData() {
        lifecycleScope.launch {
            currentGoal = db.savingsGoalDao().getSavingsGoalById(goalId)
            currentGoal?.let { goal ->
                binding.goalEmoji.text = goal.icon
                binding.goalName.text = goal.name
                binding.currentAmountText.text = "R${"%.2f".format(goal.currentAmount)}"
                binding.targetAmountText.text = "R${"%.2f".format(goal.targetAmount)} Target"
                
                val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount * 100).toInt() else 0
                binding.boostProgress.progress = progress.coerceAtMost(100)
                binding.boostProgress.setIndicatorColor(Color.parseColor(goal.color))
            }
        }
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener { finish() }

        binding.btnAddFunds.setOnClickListener {
            val amountStr = binding.addFundsEdit.text.toString()
            val amountToAdd = amountStr.toDoubleOrNull() ?: 0.0

            if (amountToAdd > 0 && currentGoal != null) {
                lifecycleScope.launch {
                    val updatedGoal = currentGoal!!.copy(
                        currentAmount = currentGoal!!.currentAmount + amountToAdd
                    )
                    db.savingsGoalDao().update(updatedGoal)
                    Toast.makeText(this@BoostGoalActivity, "Funds added successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
