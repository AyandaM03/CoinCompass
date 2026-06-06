package com.example.coincompass.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.coincompass.R
import com.example.coincompass.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make splash immersive/edge-to-edge
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start with components hidden/offset for animation
        binding.logoCard.alpha = 0f
        binding.logoCard.scaleX = 0.5f
        binding.logoCard.scaleY = 0.5f
        
        binding.appName.alpha = 0f
        binding.appName.translationY = 50f
        
        binding.slogan.alpha = 0f
        binding.slogan.translationY = 30f

        // Animate the logo
        binding.logoCard.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Animate the app name
        binding.appName.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(500)
            .setDuration(800)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Animate the slogan
        binding.slogan.animate()
            .alpha(0.8f)
            .translationY(0f)
            .setStartDelay(800)
            .setDuration(800)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // Move to the next screen after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val isRemembered = sharedPref.getBoolean("remember_me", false)
            
            val targetActivity = if (isRemembered) {
                com.example.coincompass.MainActivity::class.java
            } else {
                LoginActivity::class.java
            }
            
            startActivity(Intent(this, targetActivity))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 3000)
    }
}
