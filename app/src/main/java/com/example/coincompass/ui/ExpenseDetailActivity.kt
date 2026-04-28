package com.example.coincompass.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Expense
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
                binding.detailAmount.text = "R${"%.2f".format(expense.amount)}"
                binding.detailDescription.text = expense.description
                binding.detailDateTime.text = "${expense.date} | ${expense.startTime} - ${expense.endTime}"

                if (!expense.photoPath.isNullOrEmpty()) {
                    binding.detailImage.visibility = View.VISIBLE
                    binding.detailImage.setImageURI(Uri.parse(expense.photoPath))
                }
            }
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}
