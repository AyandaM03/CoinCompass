package com.example.coincompass.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Expense
import com.example.coincompass.databinding.ActivityAddExpenseBinding
import kotlinx.coroutines.launch

// This class is where we add new money spent (expenses)
class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init our database
        db = AppDatabase.getDatabase(this)

        // Load categories into the dropdown menu
        db.categoryDao().getAllCategories().observe(this) { categories ->
            val categoryNames = categories.map { it.name }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.categorySpinner.adapter = adapter
        }

        // Close button logic
        binding.btnClose.setOnClickListener {
            finish() // Just go back to main screen
        }

        // When we click "Save Expense"
        binding.saveExpenseButton.setOnClickListener {
            val category = binding.categorySpinner.selectedItem?.toString() ?: ""
            val desc = binding.descriptionEdit.text.toString()
            val amountStr = binding.amountEdit.text.toString()
            val date = binding.dateEdit.text.toString()
            val startTime = binding.startTimeEdit.text.toString()
            val endTime = binding.endTimeEdit.text.toString()

            // Check if all needed stuff is filled
            if (category.isNotEmpty() && amountStr.isNotEmpty() && date.isNotEmpty()) {
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                
                // Save it in the background so it doesn't freeze the screen
                lifecycleScope.launch {
                    val expense = Expense(
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        description = desc,
                        categoryName = category,
                        amount = amount
                    )
                    db.expenseDao().insert(expense)
                    Toast.makeText(this@AddExpenseActivity, "Expense saved!", Toast.LENGTH_SHORT).show()
                    finish() // Close this screen
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
