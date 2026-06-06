package com.example.coincompass.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Expense
import com.example.coincompass.databinding.ActivityAddExpenseBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var db: AppDatabase
    private var selectedImageUri: String? = null
    private val calendar = Calendar.getInstance()

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri.toString()
            binding.receiptPreview.visibility = android.view.View.VISIBLE
            binding.receiptPreview.setImageURI(uri)
            binding.btnAttachReceipt.text = "Change Receipt Photo"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        db.categoryDao().getAllCategories().observe(this) { categories ->
            val categoryNames = categories.map { it.name }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.categorySpinner.adapter = adapter
        }

        binding.dateEdit.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.dateEdit.setText(format.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.startTimeEdit.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                binding.startTimeEdit.setText(String.format("%02d:%02d", hourOfDay, minute))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        binding.endTimeEdit.setOnClickListener {
            TimePickerDialog(this, { _, hourOfDay, minute ->
                binding.endTimeEdit.setText(String.format("%02d:%02d", hourOfDay, minute))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnAttachReceipt.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.saveExpenseButton.setOnClickListener {
            val category = binding.categorySpinner.selectedItem?.toString() ?: ""
            val desc = binding.descriptionEdit.text.toString()
            val amountStr = binding.amountEdit.text.toString()
            val date = binding.dateEdit.text.toString()
            val startTime = binding.startTimeEdit.text.toString()
            val endTime = binding.endTimeEdit.text.toString()
            
            val type = if (binding.typeToggleGroup.checkedButtonId == R.id.btn_type_income) "Income" else "Expense"

            if (category.isNotEmpty() && amountStr.isNotEmpty() && date.isNotEmpty()) {
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                
                lifecycleScope.launch {
                    val expense = Expense(
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        description = desc,
                        categoryName = category,
                        amount = amount,
                        type = type,
                        photoPath = selectedImageUri
                    )
                    db.expenseDao().insert(expense)
                    Toast.makeText(this@AddExpenseActivity, "Transaction saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
