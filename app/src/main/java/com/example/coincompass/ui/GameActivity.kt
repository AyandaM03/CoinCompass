package com.example.coincompass.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.RewardPoints
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random

class GameActivity : AppCompatActivity() {
    private lateinit var gameLayout: FrameLayout
    private lateinit var scoreText: TextView
    private lateinit var timerText: TextView
    private lateinit var db: AppDatabase
    
    private var score = 0
    private var timeLeft = 30
    
    // Combo system
    private var comboCount = 0
    private var lastCoinTime = 0L
    private val comboWindow = 1200L
    private val handler = Handler(Looper.getMainLooper())
    private val activeCoins = mutableListOf<ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        
        db = AppDatabase.getDatabase(this)
        gameLayout = findViewById(R.id.gameLayout)
        scoreText = findViewById(R.id.scoreText)
        timerText = findViewById(R.id.timerText)
        
        startCountdown()
    }

    private fun startCountdown() {
        val countdownText = TextView(this).apply {
            text = "3"
            textSize = 100f
            setTextColor(ContextCompat.getColor(this@GameActivity, R.color.primary_green))
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.gravity = Gravity.CENTER }
        }
        gameLayout.addView(countdownText)
        
        var countdown = 3
        handler.post(object : Runnable {
            override fun run() {
                countdownText.scaleX = 1.8f
                countdownText.scaleY = 1.8f
                countdownText.alpha = 1f
                countdownText.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(400)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
                
                if (countdown > 0) {
                    countdownText.text = countdown.toString()
                    countdown--
                    handler.postDelayed(this, 1000)
                } else {
                    countdownText.text = "GO!"
                    countdownText.textSize = 56f
                    handler.postDelayed({
                        countdownText.animate().alpha(0f).setDuration(300).withEndAction {
                            gameLayout.removeView(countdownText)
                        }.start()
                        startTimer()
                        spawnCoins()
                    }, 700)
                }
            }
        })
    }

    private fun startTimer() {
        handler.post(object : Runnable {
            override fun run() {
                if (timeLeft > 0) {
                    timeLeft--
                    timerText.text = timeLeft.toString()
                    if (timeLeft <= 10) {
                        timerText.setTextColor(ContextCompat.getColor(this@GameActivity, R.color.expense_red))
                        timerText.animate()
                            .scaleX(1.3f).scaleY(1.3f).setDuration(120)
                            .withEndAction {
                                timerText.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                            }.start()
                    }
                    handler.postDelayed(this, 1000)
                } else {
                    endGame()
                }
            }
        })
    }

    private fun spawnCoins() {
        handler.post(object : Runnable {
            override fun run() {
                if (timeLeft > 0) {
                    createFallingCoin()
                    val interval = when {
                        timeLeft > 20 -> 1200L
                        timeLeft > 10 -> 900L
                        else -> 650L
                    }
                    handler.postDelayed(this, interval)
                }
            }
        })
    }

    private fun createFallingCoin() {
        val coin = ImageView(this).apply {
            setImageResource(R.drawable.coin_bag)
            val size = 160
            layoutParams = FrameLayout.LayoutParams(size, size).also {
                it.leftMargin = Random.nextInt(60, gameLayout.width - 200)
                it.topMargin = -200
            }
        }
        gameLayout.addView(coin)
        activeCoins.add(coin)

        val fallDuration = when {
            timeLeft > 20 -> 2700L
            timeLeft > 10 -> 2200L
            else -> 1700L
        }
        
        val animator = ObjectAnimator.ofFloat(coin, "translationY", 0f, gameLayout.height.toFloat() + 200).apply {
            duration = fallDuration
            interpolator = LinearInterpolator()
            start()
        }

        coin.setOnClickListener {
            coin.isClickable = false
            animator.cancel()
            activeCoins.remove(coin)
            
            val now = System.currentTimeMillis()
            comboCount = if (now - lastCoinTime < comboWindow) comboCount + 1 else 1
            lastCoinTime = now
            
            val pointsEarned = comboCount
            score += pointsEarned
            scoreText.text = score.toString()
            
            spawnFloatingText(coin, pointsEarned)
            spawnParticles(coin)
            showComboLabel()
            
            coin.animate()
                .scaleX(1.5f).scaleY(1.5f).alpha(0f)
                .setDuration(160)
                .withEndAction { gameLayout.removeView(coin) }
                .start()
        }

        handler.postDelayed({
            if (coin.parent != null) {
                gameLayout.removeView(coin)
                activeCoins.remove(coin)
            }
        }, fallDuration)
    }

    private fun spawnFloatingText(anchor: ImageView, points: Int) {
        val loc = IntArray(2)
        anchor.getLocationInWindow(loc)
        val label = if (comboCount > 1) "+$points x$comboCount COMBO!" else "+$points"
        val color = if (comboCount > 1) ContextCompat.getColor(this, R.color.gold_accent) else ContextCompat.getColor(this, R.color.primary_green)
        
        val floatText = TextView(this).apply {
            text = label
            textSize = if (comboCount > 1) 22f else 18f
            setTextColor(color)
            isSingleLine = true
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also {
                it.leftMargin = loc[0]
                it.topMargin = loc[1] - 100
            }
        }
        gameLayout.addView(floatText)
        floatText.animate()
            .translationY(-180f).alpha(0f)
            .setDuration(900)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { gameLayout.removeView(floatText) }
            .start()
    }

    private fun spawnParticles(anchor: ImageView) {
        val loc = IntArray(2)
        anchor.getLocationInWindow(loc)
        val cx = loc[0] + 80f
        val cy = loc[1] + 80f
        val colors = listOf(
            ContextCompat.getColor(this, R.color.primary_green),
            ContextCompat.getColor(this, R.color.gold_accent),
            ContextCompat.getColor(this, R.color.cat_transport),
            Color.WHITE
        )
        
        repeat(8) { i ->
            val dot = TextView(this).apply {
                text = "●"
                textSize = 12f
                setTextColor(colors[i % colors.size])
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also {
                    it.leftMargin = cx.toInt()
                    it.topMargin = cy.toInt()
                }
            }
            gameLayout.addView(dot)
            val angle = (i * 45.0) * Math.PI / 180
            val dist = Random.nextInt(100, 200).toFloat()
            val tx = (Math.cos(angle) * dist).toFloat()
            val ty = (Math.sin(angle) * dist).toFloat()
            
            dot.animate()
                .translationX(tx).translationY(ty).alpha(0f).scaleX(0f).scaleY(0f)
                .setDuration(500)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction { gameLayout.removeView(dot) }
                .start()
        }
    }

    private var comboLabel: TextView? = null
    private fun showComboLabel() {
        if (comboCount < 2) {
            comboLabel?.let {
                it.animate().alpha(0f).setDuration(200).withEndAction { gameLayout.removeView(it) }.start()
                comboLabel = null
            }
            return
        }
        val existing = comboLabel
        if (existing != null) {
            existing.text = "🔥 x$comboCount COMBO"
            existing.animate().cancel()
            existing.scaleX = 1.3f
            existing.scaleY = 1.3f
            existing.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
        } else {
            val lbl = TextView(this).apply {
                text = "🔥 x$comboCount COMBO"
                textSize = 20f
                setTextColor(ContextCompat.getColor(this@GameActivity, R.color.gold_accent))
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also {
                    it.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    it.bottomMargin = 120
                }
            }
            gameLayout.addView(lbl)
            comboLabel = lbl
        }
    }

    private fun endGame() {
        handler.removeCallbacksAndMessages(null)
        activeCoins.toList().forEach { if (it.parent != null) gameLayout.removeView(it) }
        activeCoins.clear()
        comboLabel?.let { gameLayout.removeView(it) }
        
        val pointsEarned = score * 10
        
        lifecycleScope.launch {
            val currentStats = db.rewardPointsDao().getRewardPointsSync() ?: RewardPoints()
            val newTotalPoints = currentStats.points + pointsEarned
            val newGamesPlayed = currentStats.totalGamesPlayed + 1
            
            val newLevel = when {
                newTotalPoints >= 1000 -> 5
                newTotalPoints >= 500 -> 4
                newTotalPoints >= 250 -> 3
                newTotalPoints >= 100 -> 2
                else -> 1
            }
            
            // Badge logic
            val earnedBadges = currentStats.badgesEarned.split(",").filter { it.isNotEmpty() }.toMutableSet()
            if (newGamesPlayed >= 1) earnedBadges.add("First Step")
            if (newTotalPoints >= 100) earnedBadges.add("Budget Beginner")
            if (newTotalPoints >= 500) earnedBadges.add("Savings Star")
            if (newTotalPoints >= 1000) earnedBadges.add("Coin Master")
            if (newTotalPoints >= 2500) earnedBadges.add("Financial Champion")
            
            val updatedStats = currentStats.copy(
                points = newTotalPoints,
                totalGamesPlayed = newGamesPlayed,
                level = newLevel,
                badgesEarned = earnedBadges.joinToString(",")
            )
            db.rewardPointsDao().insertOrUpdate(updatedStats)
            
            showGameOverScreen(score, pointsEarned, newLevel, newTotalPoints)
        }
    }

    private fun showGameOverScreen(finalScore: Int, points: Int, level: Int, totalPoints: Int) {
        gameLayout.removeAllViews()
        
        val nextLevelThreshold = when(level) {
            1 -> 100
            2 -> 250
            3 -> 500
            4 -> 1000
            else -> totalPoints
        }
        
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
            setPadding(48, 48, 48, 48)
        }

        val card = com.google.android.material.card.MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setCardBackgroundColor(Color.WHITE)
            radius = 32f
            cardElevation = 16f
            useCompatPadding = true
        }
        
        val cardContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(60, 60, 60, 60)
        }

        cardContent.addView(TextView(this).apply {
            text = "GAME OVER"
            textSize = 32f
            setTextColor(ContextCompat.getColor(this@GameActivity, R.color.primary_green))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        })

        cardContent.addView(TextView(this).apply {
            text = "Final Score: $finalScore"
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@GameActivity, R.color.dark_text))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        })

        cardContent.addView(TextView(this).apply {
            text = "+$points Reward Points"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@GameActivity, R.color.gold_accent))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 36)
        })

        cardContent.addView(TextView(this).apply {
            text = "Current Level: $level"
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@GameActivity, R.color.primary_green))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        })

        val xpBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = nextLevelThreshold
            progress = totalPoints
            progressTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this@GameActivity, R.color.gold_accent))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 30
            ).also { it.setMargins(0, 16, 0, 8) }
        }
        cardContent.addView(xpBar)

        cardContent.addView(TextView(this).apply {
            text = if (level < 5) "$totalPoints / $nextLevelThreshold to Level ${level + 1}" else "MAX LEVEL REACHED"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@GameActivity, R.color.text_grey))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 48)
        })

        val btnPlayAgain = com.google.android.material.button.MaterialButton(this).apply {
            text = "PLAY AGAIN"
            setBackgroundColor(ContextCompat.getColor(this@GameActivity, R.color.gold_accent))
            setTextColor(Color.WHITE)
            cornerRadius = 24
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, 0, 0, 16) }
            setOnClickListener { restartGame() }
        }
        cardContent.addView(btnPlayAgain)

        val btnDashboard = com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "RETURN TO DASHBOARD"
            setTextColor(ContextCompat.getColor(this@GameActivity, R.color.primary_green))
            setStrokeColorResource(R.color.primary_green)
            strokeWidth = 4
            cornerRadius = 24
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { finish() }
        }
        cardContent.addView(btnDashboard)

        card.addView(cardContent)
        container.addView(card)
        gameLayout.addView(container)

        container.alpha = 0f
        container.translationY = 60f
        container.animate()
            .alpha(1f).translationY(0f)
            .setDuration(400)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
            
        ObjectAnimator.ofInt(xpBar, "progress", 0, totalPoints).apply {
            duration = 1200
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun restartGame() {
        score = 0
        timeLeft = 30
        comboCount = 0
        lastCoinTime = 0L
        activeCoins.clear()
        comboLabel = null
        
        // Wipe everything and rebuild the clean game UI
        gameLayout.removeAllViews()
        
        val inflater = LayoutInflater.from(this)
        // Inflate the base game layout
        val root = inflater.inflate(R.layout.activity_game, null) as FrameLayout
        // Remove the inner gameLayout if it exists to prevent recursion, or just take children
        val children = mutableListOf<View>()
        for (i in 0 until root.childCount) {
            children.add(root.getChildAt(i))
        }
        root.removeAllViews()
        for (child in children) {
            gameLayout.addView(child)
        }
        
        scoreText = findViewById(R.id.scoreText)
        timerText = findViewById(R.id.timerText)
        
        startCountdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
