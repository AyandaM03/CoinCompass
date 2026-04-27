package com.example.coincompass.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Goal
import com.example.coincompass.databinding.ActivitySetGoalBinding
import kotlinx.coroutines.launch

class SetGoalActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySetGoalBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)

        binding.saveButton.setOnClickListener {
            val month = binding.monthEdit.text.toString()
            val min = binding.minGoalEdit.text.toString().toDoubleOrNull() ?: 0.0
            val max = binding.maxGoalEdit.text.toString().toDoubleOrNull() ?: 0.0

            if (month.isEmpty()) {
                Toast.makeText(this, "Please enter a month", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                db.goalDao().insertOrUpdate(Goal(month = month, minGoal = min, maxGoal = max))
                Toast.makeText(this@SetGoalActivity, "Goal saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
