package com.example.coincompass.ui

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.databinding.ActivityExpenseDetailBinding
import kotlinx.coroutines.launch

class ExpenseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityExpenseDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val expenseId = intent.getLongExtra("expense_id", -1)
            if (expenseId == -1L) {
                finish()
                return
            }

            val db = AppDatabase.getDatabase(this)
            lifecycleScope.launch {
                try {
                    val expense = db.expenseDao().getExpenseById(expenseId)
                    if (expense != null) {
                        binding.detailCategory.text = expense.categoryName
                        binding.detailDescription.text = expense.description
                        binding.detailDate.text = expense.date
                        binding.detailTime.text = "${expense.startTime} - ${expense.endTime}"
                        binding.detailType.text = expense.type

                        if (expense.type == "Income") {
                            binding.detailAmount.text = "+R${"%.2f".format(expense.amount)}"
                            binding.detailAmount.setTextColor(ContextCompat.getColor(this@ExpenseDetailActivity, R.color.primary_green))
                            binding.detailTypeIcon.setImageResource(R.drawable.ic_add)
                            binding.detailTypeIcon.setBackgroundColor(ContextCompat.getColor(this@ExpenseDetailActivity, R.color.soft_mint))
                        } else {
                            binding.detailAmount.text = "-R${"%.2f".format(expense.amount)}"
                            binding.detailAmount.setTextColor(ContextCompat.getColor(this@ExpenseDetailActivity, R.color.expense_red))
                            binding.detailTypeIcon.setImageResource(R.drawable.ic_history)
                            binding.detailTypeIcon.setBackgroundColor(Color.parseColor("#FFEBEE"))
                        }

                        if (!expense.photoPath.isNullOrEmpty()) {
                            binding.receiptCard.visibility = View.VISIBLE
                            if (expense.photoPath == "camera_bitmap") {
                                binding.detailImage.setImageResource(android.R.drawable.ic_menu_camera)
                            } else {
                                try {
                                    val uri = Uri.parse(expense.photoPath)
                                    // Verify permission by attempting to open stream, avoiding deferred crash in onMeasure
                                    contentResolver.openInputStream(uri)?.use { 
                                        binding.detailImage.setImageURI(uri)
                                    } ?: run {
                                        binding.receiptCard.visibility = View.GONE
                                    }
                                } catch (e: Exception) {
                                    Log.e("ExpenseDetailActivity", "Error loading image", e)
                                    binding.receiptCard.visibility = View.GONE
                                }
                            }
                        } else {
                            binding.receiptCard.visibility = View.GONE
                        }
                        
                        binding.btnDelete.setOnClickListener {
                            lifecycleScope.launch {
                                try {
                                    db.expenseDao().delete(expense)
                                    Toast.makeText(this@ExpenseDetailActivity, "Transaction deleted", Toast.LENGTH_SHORT).show()
                                    finish()
                                } catch (e: Exception) {
                                    Log.e("ExpenseDetailActivity", "Error deleting", e)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ExpenseDetailActivity", "Error loading data", e)
                }
            }

            binding.btnBack.setOnClickListener {
                finish()
            }
        } catch (e: Exception) {
            Log.e("ExpenseDetailActivity", "Error in onCreate", e)
        }
    }
}
