package com.example.coincompass.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    private val calendar = Calendar.getInstance()
    private var selectedImageUri: String? = null
    private var editExpenseId: Long = -1

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            handleImageResult(uri)
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                binding.receiptPreview.visibility = View.VISIBLE
                binding.receiptPreview.setImageBitmap(bitmap)
                selectedImageUri = "camera_bitmap" 
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        editExpenseId = intent.getLongExtra("expense_id", -1)

        setupCategoryDropdown()
        setupListeners()
        setupRealTimePreview()
        
        if (isEditMode()) {
            loadExpenseData()
        }
    }

    private fun isEditMode() = editExpenseId != -1L

    private fun loadExpenseData() {
        binding.headerTitle.text = "Edit Transaction"
        binding.saveExpenseButton.text = "Update Transaction"
        
        lifecycleScope.launch {
            val expense = db.expenseDao().getExpenseById(editExpenseId)
            expense?.let {
                binding.amountEdit.setText(it.amount.toString())
                binding.categorySpinnerText.setText(it.categoryName, false)
                binding.dateEdit.setText(it.date)
                binding.notesEdit.setText(it.description)
                binding.locationEdit.setText(it.startTime)
                
                if (it.type == "Income") {
                    binding.typeToggleGroup.check(R.id.btn_type_income)
                } else {
                    binding.typeToggleGroup.check(R.id.btn_type_expense)
                }
                
                if (it.photoPath != null) {
                    selectedImageUri = it.photoPath
                    if (it.photoPath != "camera_bitmap") {
                        binding.receiptPreview.visibility = View.VISIBLE
                        binding.receiptPreview.setImageURI(Uri.parse(it.photoPath))
                    }
                }
                
                updatePreviewFromInputs()
                updatePreviewColors(binding.typeToggleGroup.checkedButtonId)
            }
        }
    }

    private fun setupCategoryDropdown() {
        db.categoryDao().getAllCategories().observe(this) { categories ->
            val categoryNames = categories.map { it.name }
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
            binding.categorySpinnerText.setAdapter(adapter)
        }
    }

    private fun setupListeners() {
        binding.dateEdit.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateDisplay()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnGallery.setOnClickListener { pickImage.launch("image/*") }

        binding.btnCamera.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePhoto.launch(intent)
        }

        binding.typeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) updatePreviewColors(checkedId)
        }

        binding.saveExpenseButton.setOnClickListener {
            saveTransaction()
        }
    }

    private fun updateDateDisplay() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = format.format(calendar.time)
        binding.dateEdit.setText(dateStr)
        binding.previewDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
    }

    private fun setupRealTimePreview() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreviewFromInputs()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.amountEdit.addTextChangedListener(watcher)
        binding.categorySpinnerText.addTextChangedListener(watcher)
        
        updateDateDisplay()
    }

    private fun updatePreviewFromInputs() {
        val amountStr = binding.amountEdit.text.toString()
        binding.previewAmount.text = if (amountStr.isEmpty()) "R 0.00" else "R $amountStr"
        
        val category = binding.categorySpinnerText.text.toString()
        binding.previewCategoryName.text = if (category.isEmpty()) "Select Category" else category

        val iconRes = when (category.lowercase()) {
            "groceries" -> R.drawable.ic_categories
            "food", "food & dining" -> R.drawable.ic_categories
            "transport" -> R.drawable.ic_calendar
            "entertainment" -> R.drawable.ic_analytics
            "salary", "freelance" -> R.drawable.ic_add
            "investments" -> R.drawable.ic_analytics
            "savings" -> R.drawable.ic_goals
            else -> R.drawable.ic_categories
        }
        binding.previewCategoryIcon.setImageResource(iconRes)
    }

    private fun updatePreviewColors(checkedId: Int) {
        if (checkedId == R.id.btn_type_income) {
            binding.previewAmount.setTextColor(ContextCompat.getColor(this, R.color.income_green))
        } else {
            binding.previewAmount.setTextColor(ContextCompat.getColor(this, R.color.expense_red))
        }
    }

    private fun handleImageResult(uri: Uri) {
        selectedImageUri = uri.toString()
        binding.receiptPreview.visibility = View.VISIBLE
        binding.receiptPreview.setImageURI(uri)
    }

    private fun saveTransaction() {
        val category = binding.categorySpinnerText.text.toString()
        val amountStr = binding.amountEdit.text.toString()
        val date = binding.dateEdit.text.toString()
        val notes = binding.notesEdit.text.toString()
        val location = binding.locationEdit.text.toString()
        
        val type = if (binding.typeToggleGroup.checkedButtonId == R.id.btn_type_income) "Income" else "Expense"

        if (category.isNotEmpty() && amountStr.isNotEmpty() && date.isNotEmpty()) {
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            
            lifecycleScope.launch {
                val expense = Expense(
                    id = if (isEditMode()) editExpenseId else 0,
                    date = date,
                    startTime = location, 
                    endTime = "",
                    description = notes,
                    categoryName = category,
                    amount = amount,
                    type = type,
                    photoPath = selectedImageUri
                )
                
                if (isEditMode()) {
                    db.expenseDao().update(expense)
                } else {
                    db.expenseDao().insert(expense)
                }

                Toast.makeText(this@AddExpenseActivity, "Transaction saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "Please fill in required fields", Toast.LENGTH_SHORT).show()
        }
    }
}
