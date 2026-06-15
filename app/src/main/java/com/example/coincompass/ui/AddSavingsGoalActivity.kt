package com.example.coincompass.ui

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.SavingsGoal
import com.example.coincompass.databinding.ActivityAddSavingsGoalBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddSavingsGoalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddSavingsGoalBinding
    private lateinit var db: AppDatabase
    private val calendar = Calendar.getInstance()
    
    private var selectedIcon = "💰"
    private var selectedColor = "#2E7D32"
    private var editGoalId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddSavingsGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)
        editGoalId = intent.getLongExtra("goal_id", -1)

        setupIconPicker()
        setupColorPicker()
        setupListeners()
        
        if (isEditMode()) {
            loadGoalData()
        } else {
            updatePreview()
        }
    }

    private fun isEditMode() = editGoalId != -1L

    private fun loadGoalData() {
        binding.headerTitle.text = "Edit Savings Goal"
        binding.btnCreateGoal.text = "Update Goal"
        
        lifecycleScope.launch {
            val goal = db.savingsGoalDao().getSavingsGoalById(editGoalId)
            goal?.let {
                binding.goalNameEdit.setText(it.name)
                binding.targetAmountEdit.setText(it.targetAmount.toString())
                binding.targetDateEdit.setText(it.deadline)
                selectedIcon = it.icon
                selectedColor = it.color
                updatePreview()
            }
        }
    }

    private fun setupIconPicker() {
        val icons = listOf("🚗", "🏠", "✈", "🎓", "💻", "💰", "🎮", "❤️")
        icons.forEach { icon ->
            val textView = TextView(this).apply {
                text = icon
                textSize = 28f
                setPadding(24, 16, 24, 16)
                setOnClickListener {
                    selectedIcon = icon
                    updatePreview()
                }
            }
            binding.iconPickerLayout.addView(textView)
        }
    }

    private fun setupColorPicker() {
        // Green, Yellow, Blue, Purple
        val colors = listOf("#2E7D32", "#F9A825", "#3B82F6", "#8B5CF6")
        colors.forEach { colorStr ->
            val colorView = View(this).apply {
                val size = (32 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(16, 0, 16, 0)
                }
                background = ContextCompat.getDrawable(context, R.drawable.color_picker_dot)
                backgroundTintList = ColorStateList.valueOf(Color.parseColor(colorStr))
                setOnClickListener {
                    selectedColor = colorStr
                    updatePreview()
                }
            }
            binding.colorPickerLayout.addView(colorView)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.targetDateEdit.setOnClickListener {
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                binding.targetDateEdit.setText(format.format(calendar.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.goalNameEdit.addTextChangedListener(watcher)
        binding.targetAmountEdit.addTextChangedListener(watcher)

        binding.btnCreateGoal.setOnClickListener {
            val name = binding.goalNameEdit.text.toString().trim()
            val targetStr = binding.targetAmountEdit.text.toString().trim()
            val deadline = binding.targetDateEdit.text.toString().trim()

            if (name.isNotEmpty() && targetStr.isNotEmpty()) {
                val target = targetStr.toDoubleOrNull() ?: 0.0
                lifecycleScope.launch {
                    val goal = if (isEditMode()) {
                        val existing = db.savingsGoalDao().getSavingsGoalById(editGoalId)
                        existing?.copy(
                            name = name,
                            targetAmount = target,
                            deadline = deadline,
                            icon = selectedIcon,
                            color = selectedColor
                        )
                    } else {
                        SavingsGoal(
                            name = name,
                            targetAmount = target,
                            deadline = deadline,
                            icon = selectedIcon,
                            color = selectedColor
                        )
                    }
                    
                    if (goal != null) {
                        if (isEditMode()) {
                            db.savingsGoalDao().update(goal)
                        } else {
                            db.savingsGoalDao().insert(goal)
                        }
                        val msg = if (isEditMode()) "Savings goal updated!" else "Savings goal created!"
                        Toast.makeText(this@AddSavingsGoalActivity, msg, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } else {
                Toast.makeText(this, "Please provide a name and target amount", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePreview() {
        binding.previewIcon.text = selectedIcon
        binding.previewName.text = binding.goalNameEdit.text.toString().ifEmpty { "New Goal" }
        
        val amount = binding.targetAmountEdit.text.toString().ifEmpty { "0" }
        binding.previewTarget.text = "R$amount Goal"
        
        val colorInt = Color.parseColor(selectedColor)
        binding.previewProgress.setIndicatorColor(colorInt)
        binding.previewPercent.setTextColor(colorInt)
    }
}
