package com.example.coincompass.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
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

/**
 * Activity for managing financial records (Expenses and Income).
 * Supports creation, modification, and attachment of receipt documentation.
 */
class AddExpenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var db: AppDatabase
    private val calendar = Calendar.getInstance()
    private var selectedImageUri: String? = null
    private var editExpenseId: Long = -1
    private var selectedType = "Expense"

    // Launcher for handling image selection from external document providers
    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            handleImageResult(uri)
        }
    }

    // Launcher for capturing receipt images using the system camera
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
        
        // Check for record ID to determine if activity is in Edit Mode
        editExpenseId = intent.getLongExtra("expense_id", -1)

        setupCategoryDropdown()
        setupListeners()
        setupRealTimePreview()
        
        if (isEditMode()) {
            loadExpenseData()
        } else {
            updateTypeSelectorUI()
        }
    }

    private fun isEditMode() = editExpenseId != -1L

    // Fetches existing record details and populates form fields for modification
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
                
                selectedType = it.type
                updateTypeSelectorUI()
                
                if (it.photoPath != null) {
                    selectedImageUri = it.photoPath
                    if (it.photoPath != "camera_bitmap") {
                        binding.receiptPreview.visibility = View.VISIBLE
                        binding.receiptPreview.setImageURI(Uri.parse(it.photoPath))
                    }
                }
                
                updatePreviewFromInputs()
            }
        }
    }

    // Populates the category AutoCompleteTextView with data from the Room database
    private fun setupCategoryDropdown() {
        db.categoryDao().getAllCategories().observe(this) { categories ->
            val categoryNames = categories.map { it.name }
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
            binding.categorySpinnerText.setAdapter(adapter)
        }
    }

    // Initialization of interactive UI components and their event handlers
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

        binding.btnGallery.setOnClickListener { pickImage.launch(arrayOf("image/*")) }

        binding.btnCamera.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePhoto.launch(intent)
        }

        binding.cardTypeExpense.setOnClickListener {
            selectedType = "Expense"
            updateTypeSelectorUI()
        }

        binding.cardTypeIncome.setOnClickListener {
            selectedType = "Income"
            updateTypeSelectorUI()
        }

        binding.saveExpenseButton.setOnClickListener {
            saveTransaction()
        }
    }

    // Updates the visual state of the transaction type selection cards
    private fun updateTypeSelectorUI() {
        val primaryGreen = ContextCompat.getColor(this, R.color.primary_green)
        val goldAccent = ContextCompat.getColor(this, R.color.gold_accent)
        val white = Color.WHITE
        val textGrey = ContextCompat.getColor(this, R.color.text_grey)

        if (selectedType == "Expense") {
            binding.cardTypeExpense.apply {
                setCardBackgroundColor(goldAccent)
                cardElevation = 12f
                animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start()
            }
            binding.iconExpense.setColorFilter(white)
            binding.textExpense.setTextColor(white)

            binding.cardTypeIncome.apply {
                setCardBackgroundColor(white)
                cardElevation = 2f
                animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
            }
            binding.iconIncome.setColorFilter(textGrey)
            binding.textIncome.setTextColor(textGrey)
        } else {
            binding.cardTypeIncome.apply {
                setCardBackgroundColor(primaryGreen)
                cardElevation = 12f
                animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start()
            }
            binding.iconIncome.setColorFilter(white)
            binding.textIncome.setTextColor(white)

            binding.cardTypeExpense.apply {
                setCardBackgroundColor(white)
                cardElevation = 2f
                animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
            }
            binding.iconExpense.setColorFilter(textGrey)
            binding.textExpense.setTextColor(textGrey)
        }
        updatePreviewColors()
    }

    private fun updateDateDisplay() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = format.format(calendar.time)
        binding.dateEdit.setText(dateStr)
        binding.previewDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
    }

    // Implementation of TextWatchers to provide immediate UI feedback as form data is entered
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
        
        val categoryName = binding.categorySpinnerText.text.toString()
        binding.previewCategoryName.text = if (categoryName.isEmpty()) "Select Category" else categoryName

        lifecycleScope.launch {
            val categories = db.categoryDao().getAllCategoriesList()
            val category = categories.find { it.name == categoryName }
            binding.previewCategoryEmoji.text = category?.icon ?: "📁"
        }
    }

    private fun updatePreviewColors() {
        if (selectedType == "Income") {
            binding.previewAmount.setTextColor(ContextCompat.getColor(this, R.color.income_green))
        } else {
            binding.previewAmount.setTextColor(ContextCompat.getColor(this, R.color.expense_red))
        }
    }

    // Persists access permissions for selected image URIs across application restarts
    private fun handleImageResult(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        selectedImageUri = uri.toString()
        binding.receiptPreview.visibility = View.VISIBLE
        binding.receiptPreview.setImageURI(uri)
    }

    // Persists the validated transaction entity to the local Room database
    private fun saveTransaction() {
        val category = binding.categorySpinnerText.text.toString()
        val amountStr = binding.amountEdit.text.toString()
        val date = binding.dateEdit.text.toString()
        val notes = binding.notesEdit.text.toString()

        if (category.isNotEmpty() && amountStr.isNotEmpty() && date.isNotEmpty()) {
            val amount = amountStr.toDoubleOrNull() ?: 0.0
            
            lifecycleScope.launch {
                val expense = Expense(
                    id = if (isEditMode()) editExpenseId else 0,
                    date = date,
                    startTime = "",
                    endTime = "",
                    description = notes,
                    categoryName = category,
                    amount = amount,
                    type = selectedType,
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
