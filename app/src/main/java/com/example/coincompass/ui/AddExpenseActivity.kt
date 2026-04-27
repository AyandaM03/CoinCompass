package com.example.coincompass.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Expense
import com.example.coincompass.databinding.ActivityAddExpenseBinding
import kotlinx.coroutines.launch
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private var selectedDate = ""
    private var selectedStartTime = ""
    private var selectedEndTime = ""
    private var photoUri: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            photoUri = it.toString()
            binding.expensePhoto.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)

        binding.photoButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Setup Spinner with some default categories or fetch from DB
        db.categoryDao().getAllCategories().observe(this) { categories ->
            val categoryNames = categories.map { it.name }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.categorySpinner.adapter = adapter
        }

        binding.dateButton.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate = "$year-${month + 1}-$day"
                binding.selectedDate.text = selectedDate
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.startTimeButton.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                selectedStartTime = String.format("%02d:%02d", hour, minute)
                binding.selectedStartTime.text = selectedStartTime
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        binding.endTimeButton.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                selectedEndTime = String.format("%02d:%02d", hour, minute)
                binding.selectedEndTime.text = selectedEndTime
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        binding.saveButton.setOnClickListener {
            val amount = binding.amountEdit.text.toString().toDoubleOrNull() ?: 0.0
            val description = binding.descriptionEdit.text.toString()
            val category = binding.categorySpinner.selectedItem?.toString() ?: "General"

            if (amount <= 0 || selectedDate.isEmpty()) {
                Toast.makeText(this, "Please fill in amount and date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val expense = Expense(
                date = selectedDate,
                startTime = selectedStartTime,
                endTime = selectedEndTime,
                description = description,
                categoryName = category,
                amount = amount,
                photoPath = photoUri
            )

            lifecycleScope.launch {
                db.expenseDao().insert(expense)
                Toast.makeText(this@AddExpenseActivity, "Expense saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
