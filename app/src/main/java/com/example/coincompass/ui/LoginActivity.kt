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

/**
 * Activity handling user authentication including login validation and session persistence.
 */
class LoginActivity : AppCompatActivity() {

    // ViewBinding instance for type-safe access to layout views
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(this)
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // Asynchronous database warm-up to identify potential migration issues during initialization
        lifecycleScope.launch {
            try {
                db.userDao().getUserByUsername("")
            } catch (e: Exception) {
                // Fallback to destructive migration handled by Room configuration if specified
            }
        }

        // Automatic redirection to MainActivity if the user previously selected "Remember Me"
        val isRemembered = sharedPref.getBoolean("remember_me", false)
        if (isRemembered) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Click listener for sign-in action with input validation and credential verification
        binding.signinButton.setOnClickListener {
            val username = binding.usernameEdit.text.toString().trim()
            val password = binding.passwordEdit.text.toString()

            // Validate non-empty username/email input
            if (username.isEmpty()) {
                binding.usernameLayout.error = "Please enter username"
                return@setOnClickListener
            } else {
                binding.usernameLayout.error = null
            }

            // Validate non-empty password input
            if (password.isEmpty()) {
                binding.passwordLayout.error = "Please enter password"
                return@setOnClickListener
            } else {
                binding.passwordLayout.error = null
            }

            // Execute credential check on a background thread using coroutines
            lifecycleScope.launch {
                // Attempt to resolve user entity by either username or email
                var user = db.userDao().getUserByUsername(username)
                if (user == null) {
                    user = db.userDao().getUserByEmail(username)
                }

                // Verify password hash and initialize session if valid
                if (user != null && SecurityUtils.verifyPassword(password, user.password)) {
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

        // Simulated password recovery action
        binding.forgotPassword.setOnClickListener {
            Toast.makeText(this, "Password reset link sent to your email (Simulated)", Toast.LENGTH_LONG).show()
        }

        // Navigation to account registration activity
        binding.signupLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
