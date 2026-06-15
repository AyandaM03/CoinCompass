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

// This is the first screen the user sees if they aren't logged in. 
// I'm using AppCompatActivity because it's the standard for modern Android apps!
class LoginActivity : AppCompatActivity() {

    // I used ViewBinding here so I don't have to use findViewById a million times. 
    // It's much cleaner!
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflating the layout using binding.
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Getting my database instance.
        val db = AppDatabase.getDatabase(this)
        // SharedPreferences is like a tiny save file for settings.
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // I'm launching a coroutine here to 'pre-warm' the database. 
        // Coroutines are great for doing stuff in the background so the app doesn't freeze!
        lifecycleScope.launch {
            try {
                db.userDao().getUserByUsername("")
            } catch (e: Exception) {
                // If something goes wrong with the database migration, we just ignore it for now.
            }
        }

        // Check if the user clicked "Remember Me" last time.
        // If they did, we just skip the login and go straight to the Main Activity!
        val isRemembered = sharedPref.getBoolean("remember_me", false)
        if (isRemembered) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // We close LoginActivity so they can't go 'back' to it.
        }

        // What happens when the user clicks 'Sign In'.
        binding.signinButton.setOnClickListener {
            val username = binding.usernameEdit.text.toString().trim()
            val password = binding.passwordEdit.text.toString()

            // Basic validation to make sure they didn't leave anything empty.
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

            // Doing the actual login check in the background.
            lifecycleScope.launch {
                // We check both username and email, just in case they forgot which one they used.
                var user = db.userDao().getUserByUsername(username)
                if (user == null) {
                    user = db.userDao().getUserByEmail(username)
                }

                // If we found a user and the password is correct...
                if (user != null && SecurityUtils.verifyPassword(password, user.password)) {
                    // Save their name and whether we should remember them.
                    val editor = sharedPref.edit()
                    editor.putString("current_username", user.fullName)
                    editor.putBoolean("remember_me", binding.rememberMeCheckbox.isChecked)
                    editor.apply()

                    // Switch to the Main Activity!
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Show a little pop-up message if they got it wrong.
                    Toast.makeText(this@LoginActivity, "Invalid email/username or password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Just a dummy action for now.
        binding.forgotPassword.setOnClickListener {
            Toast.makeText(this, "Password reset link sent to your email (Simulated)", Toast.LENGTH_LONG).show()
        }

        // If they don't have an account, they can go to the Register screen.
        binding.signupLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
