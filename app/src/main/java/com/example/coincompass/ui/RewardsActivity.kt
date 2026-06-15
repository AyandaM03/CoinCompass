package com.example.coincompass.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.coincompass.R
import com.example.coincompass.data.AppDatabase
import com.example.coincompass.data.RewardPoints
import com.example.coincompass.databinding.ActivityRewardsBinding
import com.example.coincompass.databinding.ItemBadgeBinding
import kotlinx.coroutines.launch
import java.util.*

class RewardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardsBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: BadgesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = AppDatabase.getDatabase(this)

        setupRecyclerView()
        observeData()

        binding.btnPlayNow.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        binding.btnBackManual.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = BadgesAdapter()
        binding.badgesRecycler.layoutManager = GridLayoutManager(this, 3)
        binding.badgesRecycler.adapter = adapter
    }

    private fun observeData() {
        db.rewardPointsDao().getRewardPoints().observe(this) { stats ->
            val rewardStats = stats ?: RewardPoints()
            updateUI(rewardStats)
        }
    }

    private fun updateUI(stats: RewardPoints) {
        binding.totalPointsText.text = "${stats.points}"
        binding.levelText.text = "${stats.level}"
        binding.gamesPlayedText.text = "${stats.totalGamesPlayed}"

        val nextLevelThreshold = when (stats.level) {
            1 -> 100
            2 -> 250
            3 -> 500
            4 -> 1000
            else -> stats.points
        }

        binding.levelProgressLabel.text = if (stats.level < 5) "Progress to Level ${stats.level + 1}" else "Max Level Reached"
        binding.levelProgressBar.max = nextLevelThreshold
        binding.levelProgressBar.progress = stats.points

        // Calculate Discipline Score
        calculateDisciplineScore(stats)

        // Setup Badges
        val allBadges = listOf(
            Badge("First Step", "🏅", "Awarded after first game", stats.totalGamesPlayed >= 1),
            Badge("Budget Beginner", "⭐", "Reach 100 points", stats.points >= 100),
            Badge("Savings Star", "🌟", "Reach 500 points", stats.points >= 500),
            Badge("Coin Master", "👑", "Reach 1000 points", stats.points >= 1000),
            Badge("Financial Champion", "🏆", "Reach 2500 points", stats.points >= 2500)
        )
        adapter.submitList(allBadges)
    }

    private fun calculateDisciplineScore(stats: RewardPoints) {
        lifecycleScope.launch {
            val expenses = db.expenseDao().getAllExpensesList()
            val totalPoints = stats.points
            // Simplified: every 10 transactions is +50 discipline points as a proxy
            val transactionBonus = (expenses.size / 10) * 50
            
            val disciplineScore = totalPoints + transactionBonus
            binding.disciplineScoreText.text = "$disciplineScore"
        }
    }

    data class Badge(val name: String, val icon: String, val description: String, val earned: Boolean)

    inner class BadgesAdapter : RecyclerView.Adapter<BadgesAdapter.ViewHolder>() {
        private var list: List<Badge> = emptyList()

        fun submitList(newList: List<Badge>) {
            list = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.binding.badgeName.text = item.name
            holder.binding.badgeIcon.text = item.icon
            
            if (item.earned) {
                holder.binding.badgeContainer.alpha = 1.0f
                holder.binding.badgeName.setTextColor(ContextCompat.getColor(this@RewardsActivity, R.color.dark_text))
            } else {
                holder.binding.badgeContainer.alpha = 0.3f
                holder.binding.badgeName.setTextColor(ContextCompat.getColor(this@RewardsActivity, R.color.text_grey))
            }
        }

        override fun getItemCount() = list.size

        inner class ViewHolder(val binding: ItemBadgeBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
