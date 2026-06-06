package com.example.coincompass.ui

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
        binding = ActivityExpenseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val expenseId = intent.getLongExtra("expense_id", -1)
        if (expenseId == -1L) {
            finish()
            return
        }

        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            val expense = db.expenseDao().getExpenseById(expenseId)
            if (expense != null) {
                binding.detailCategory.text = expense.categoryName
                binding.detailDescription.text = expense.description
                binding.detailDate.text = expense.date
                binding.detailTime.text = "${expense.startTime} - ${expense.endTime}"
                binding.detailType.text = expense.type

                if (expense.type == "Income") {
                    binding.detailAmount.text = "+R${"%.2f".format(expense.amount)}"
                    binding.detailAmount.setTextColor(ContextCompat.getColor(this@ExpenseDetailActivity, R.color.mid_green))
                    binding.detailTypeIcon.setImageResource(R.drawable.ic_add)
                    binding.detailTypeIcon.setBackgroundColor(ContextCompat.getColor(this@ExpenseDetailActivity, R.color.light_green))
                } else {
                    binding.detailAmount.text = "-R${"%.2f".format(expense.amount)}"
                    binding.detailAmount.setTextColor(ContextCompat.getColor(this@ExpenseDetailActivity, R.color.delete_red))
                    binding.detailTypeIcon.setImageResource(R.drawable.ic_history)
                    binding.detailTypeIcon.setBackgroundColor(Color.parseColor("#FFEBEE"))
                }

                if (!expense.photoPath.isNullOrEmpty()) {
                    binding.receiptCard.visibility = View.VISIBLE
                    binding.detailImage.setImageURI(Uri.parse(expense.photoPath))
                }
                
                binding.btnDelete.setOnClickListener {
                    lifecycleScope.launch {
                        db.expenseDao().delete(expense)
                        Toast.makeText(this@ExpenseDetailActivity, "Transaction deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
