package com.example.coincompass.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.Category
import com.example.coincompass.databinding.ActivityAddCategoryBinding
import com.example.coincompass.databinding.ItemCategoryBinding
import kotlinx.coroutines.launch

// This class helps users manage their expense categories
class AddCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCategoryBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup our database instance
        db = AppDatabase.getDatabase(this)

        // Set up the list that shows all categories
        adapter = CategoryAdapter()
        binding.categoriesRecycler.layoutManager = LinearLayoutManager(this)
        binding.categoriesRecycler.adapter = adapter

        // Listen for new categories being added
        db.categoryDao().getAllCategories().observe(this) { categories ->
            adapter.submitList(categories)
        }

        // When the "Add Category" button is clicked
        binding.addCategoryButton.setOnClickListener {
            val name = binding.categoryNameEdit.text.toString().trim()

            if (name.isNotEmpty()) {
                // Save to database in a background thread
                lifecycleScope.launch {
                    val newCategory = Category(name = name)
                    db.categoryDao().insert(newCategory)
                    binding.categoryNameEdit.text.clear() // Clear the input after saving
                    Toast.makeText(this@AddCategoryActivity, "Category saved!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            }
        }

        // Back button to go to previous screen
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    // Inner class for the category list adapter
    inner class CategoryAdapter : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {
        private var list: List<Category> = emptyList()

        fun submitList(newList: List<Category>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.binding.categoryName.text = item.name
            
            // When we click the X button, delete the category from the database
            holder.binding.btnDelete.setOnClickListener {
                lifecycleScope.launch {
                    db.categoryDao().delete(item) // Actually remove it!
                    Toast.makeText(this@AddCategoryActivity, "${item.name} removed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
