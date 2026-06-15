package com.example.coincompass.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.MainActivity
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.databinding.ActivityLoginBinding
import com.example.coincompass.utils.SecurityUtils
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Try to pre-warm the database to catch migration issues early
        lifecycleScope.launch {
            try {
                db.userDao().getUserByUsername("")
            } catch (e: Exception) {
                // If migration fails, the database is destructive migrated by fallback
            }
        }

        // Check if "Remember Me" was previously checked
        val isRemembered = sharedPref.getBoolean("remember_me", false)
        if (isRemembered) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.signinButton.setOnClickListener {
            val username = binding.usernameEdit.text.toString().trim()
            val password = binding.passwordEdit.text.toString()

            if (username.isEmpty()) {
                binding.usernameLayout.error = "Please enter username"
                return@setOnClickListener
            } else {
                binding.usernameLayout.error = null
            }

            if (password.isEmpty()) {
                binding.passwordLayout.error = "Please enter password"
                return@setOnClickListener
            } else {
                binding.passwordLayout.error = null
            }

            lifecycleScope.launch {
                // Try finding by username first, then email
                var user = db.userDao().getUserByUsername(username)
                if (user == null) {
                    user = db.userDao().getUserByEmail(username)
                }

                if (user != null && SecurityUtils.verifyPassword(password, user.password)) {
                    // Save login state
                    val editor = sharedPref.edit()
                    editor.putString("current_username", user.fullName)
                    editor.putBoolean("remember_me", binding.rememberMeCheckbox.isChecked)
                    editor.apply()

                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Invalid email/username or password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.forgotPassword.setOnClickListener {
            Toast.makeText(this, "Password reset link sent to your email (Simulated)", Toast.LENGTH_LONG).show()
        }

        binding.signupLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
