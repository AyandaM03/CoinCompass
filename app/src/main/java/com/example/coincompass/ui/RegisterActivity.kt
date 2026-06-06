package com.example.coincompass.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.User
import com.example.coincompass.databinding.ActivityRegisterBinding
import kotlinx.coroutines.launch

// This screen is for new users to create an account
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set up view binding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)

        // When the user clicks the "Register" button
        binding.registerButton.setOnClickListener {
            val name = binding.nameEdit.text.toString()
            val username = binding.usernameEdit.text.toString()
            val email = binding.emailEdit.text.toString()
            val password = binding.passwordEdit.text.toString()
            val confirmPassword = binding.confirmPasswordEdit.text.toString()

            // Make sure all fields are filled
            if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if passwords match
            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Must agree to terms
            if (!binding.termsCheckbox.isChecked) {
                Toast.makeText(this, "Please agree to the terms", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save the new user to the database
            lifecycleScope.launch {
                val existingUser = db.userDao().getUserByUsername(username)
                if (existingUser != null) {
                    Toast.makeText(this@RegisterActivity, "Username already exists", Toast.LENGTH_SHORT).show()
                } else {
                    val user = User(fullName = name, username = username, password = password)
                    db.userDao().insert(user)
                    Toast.makeText(this@RegisterActivity, "Account created!", Toast.LENGTH_SHORT).show()
                    finish() // Go back to login screen
                }
            }
        }

        // If they already have an account, just go back to login
        binding.loginLink.setOnClickListener {
            finish()
        }
    }
}
