package com.example.coincompass.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.MainActivity
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

// This is the login screen where the user enters their username and password
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout so we can use view binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the database instance
        val db = AppDatabase.getDatabase(this)

        // When the user clicks the sign in button
        binding.signinButton.setOnClickListener {
            val username = binding.usernameEdit.text.toString()
            val password = binding.passwordEdit.text.toString()

            // Check if they forgot to fill anything in
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Look for the user in the database in a coroutine
            lifecycleScope.launch {
                val user = db.userDao().getUserByUsername(username)
                // If we found them and the password matches
                if (user != null && user.password == password) {
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Close login screen
                } else {
                    // Show an error if it didn't work
                    Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // If they don't have an account, go to register screen
        binding.signupLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
