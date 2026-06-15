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
 * This is where I let users add their expenses or income. 
 * I tried to make it look really professional with animations and a live preview!
 */
class AddExpenseActivity : AppCompatActivity() {

    // ViewBinding is a lifesaver, no more findViewByID!
    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var db: AppDatabase
    private val calendar = Calendar.getInstance() // I'll use this for the date picker.
    private var selectedImageUri: String? = null
    private var editExpenseId: Long = -1
    private var selectedType = "Expense" // Defaulting to Expense.

    // This is for picking a photo from the gallery. Android's new API makes this easy.
    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            handleImageResult(uri)
        }
    }

    // This is for taking a quick photo of a receipt!
    private val takePhoto = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as? Bitmap
            if (bitmap != null) {
                // I show the image in a small preview window.
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

        // Initializing my database.
        db = AppDatabase.getDatabase(this)
        
        // If I got an ID from the previous screen, it means we are EDITING, not adding new.
        editExpenseId = intent.getLongExtra("expense_id", -1)

        // Setting up all my UI stuff in separate functions to keep onCreate clean.
        setupCategoryDropdown()
        setupListeners()
        setupRealTimePreview()
        
        if (isEditMode()) {
            loadExpenseData() // Load the old data so the user can change it.
        } else {
            updateTypeSelectorUI() // Just show the default view.
        }
    }

    // Helper to see if we are in edit mode.
    private fun isEditMode() = editExpenseId != -1L

    // If we are editing, I pull the data from the DB and put it in the fields.
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

    // This fills the dropdown with categories I have in the database.
    private fun setupCategoryDropdown() {
        db.categoryDao().getAllCategories().observe(this) { categories ->
            val categoryNames = categories.map { it.name }
            // Using a simple adapter for the dropdown list.
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
            binding.categorySpinnerText.setAdapter(adapter)
        }
    }

    // Here I set up all the buttons.
    private fun setupListeners() {
        // I love this DatePickerDialog, it makes choosing dates so much easier.
        binding.dateEdit.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateDisplay()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnBack.setOnClickListener { finish() }

        // Choosing a receipt from the gallery.
        binding.btnGallery.setOnClickListener { pickImage.launch(arrayOf("image/*")) }

        // Taking a new receipt photo.
        binding.btnCamera.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePhoto.launch(intent)
        }

        // Clicking these cards switches between Expense and Income.
        binding.cardTypeExpense.setOnClickListener {
            selectedType = "Expense"
            updateTypeSelectorUI()
        }

        binding.cardTypeIncome.setOnClickListener {
            selectedType = "Income"
            updateTypeSelectorUI()
        }

        // The big save button!
        binding.saveExpenseButton.setOnClickListener {
            saveTransaction()
        }
    }

    // This function handles the visual changes when you switch between Expense and Income.
    private fun updateTypeSelectorUI() {
        val primaryGreen = ContextCompat.getColor(this, R.color.primary_green)
        val goldAccent = ContextCompat.getColor(this, R.color.gold_accent)
        val white = Color.WHITE
        val textGrey = ContextCompat.getColor(this, R.color.text_grey)

        if (selectedType == "Expense") {
            // I used animations here to make the cards 'pop' when selected!
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

    // Formats the date so it looks nice.
    private fun updateDateDisplay() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = format.format(calendar.time)
        binding.dateEdit.setText(dateStr)
        binding.previewDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
    }

    // This is a cool feature: as the user types, the preview card at the top updates!
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

    // Updating the preview card with the latest info.
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

    // Income is green, Expense is red. Classic!
    private fun updatePreviewColors() {
        if (selectedType == "Income") {
            binding.previewAmount.setTextColor(ContextCompat.getColor(this, R.color.income_green))
        } else {
            binding.previewAmount.setTextColor(ContextCompat.getColor(this, R.color.expense_red))
        }
    }

    // This part was tricky. I had to make sure the app can still see the image later.
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

    // Finally, saving everything to the database.
    private fun saveTransaction() {
        val category = binding.categorySpinnerText.text.toString()
        val amountStr = binding.amountEdit.text.toString()
        val date = binding.dateEdit.text.toString()
        val notes = binding.notesEdit.text.toString()

        // Making sure they filled in the important stuff.
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
                finish() // All done, go back!
            }
        } else {
            Toast.makeText(this, "Please fill in required fields", Toast.LENGTH_SHORT).show()
        }
    }
}
