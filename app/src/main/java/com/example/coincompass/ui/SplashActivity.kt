package com.example.coincompass.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.coincompass.R

// This is the first screen that shows when you open the app
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show our splash layout
        setContentView(R.layout.activity_splash)

        // Wait for 3 seconds, then move to the login screen
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // We don't want to come back here if we press back
        }, 3000) // 3000 milliseconds = 3 seconds
    }
}
