package com.example.coincompass.ui

import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.User
import com.example.coincompass.databinding.ActivityRegisterBinding
import com.example.coincompass.utils.SecurityUtils
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)

        binding.registerButton.setOnClickListener {
            val name = binding.nameEdit.text.toString().trim()
            val username = binding.usernameEdit.text.toString().trim()
            val email = binding.emailEdit.text.toString().trim()
            val password = binding.passwordEdit.text.toString()
            val confirmPassword = binding.confirmPasswordEdit.text.toString()

            if (!validateInput(name, username, email, password, confirmPassword)) {
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val existingUsername = db.userDao().getUserByUsername(username)
                val existingEmail = db.userDao().getUserByEmail(email)

                when {
                    existingUsername != null -> {
                        binding.usernameLayout.error = "Username already taken"
                    }
                    existingEmail != null -> {
                        binding.emailLayout.error = "Email already registered"
                    }
                    else -> {
                        val hashedPassword = SecurityUtils.hashPassword(password)
                        val user = User(
                            fullName = name,
                            username = username,
                            email = email,
                            password = hashedPassword
                        )
                        db.userDao().insert(user)
                        Toast.makeText(this@RegisterActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }

        binding.loginLink.setOnClickListener {
            finish()
        }
    }

    private fun validateInput(name: String, username: String, email: String, pword: String, confirmPword: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.nameLayout.error = "Name is required"
            isValid = false
        } else {
            binding.nameLayout.error = null
        }

        if (username.isEmpty()) {
            binding.usernameLayout.error = "Username is required"
            isValid = false
        } else {
            binding.usernameLayout.error = null
        }

        if (email.isEmpty()) {
            binding.emailLayout.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Invalid email format"
            isValid = false
        } else {
            binding.emailLayout.error = null
        }

        if (pword.isEmpty()) {
            binding.passwordLayout.error = "Password is required"
            isValid = false
        } else if (pword.length < 6) {
            binding.passwordLayout.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.passwordLayout.error = null
        }

        if (confirmPword != pword) {
            binding.confirmPasswordLayout.error = "Passwords do not match"
            isValid = false
        } else {
            binding.confirmPasswordLayout.error = null
        }

        if (!binding.termsCheckbox.isChecked) {
            Toast.makeText(this, "You must agree to the terms and conditions", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }
}
