package com.example.coincompass.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User)

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?
}

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories")
    fun getAllCategories(): LiveData<List<Category>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesList(): List<Category>
}

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense)

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses")
    fun getAllExpenses(): LiveData<List<Expense>>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesList(): List<Expense>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    fun getExpensesBetweenDates(startDate: String, endDate: String): LiveData<List<Expense>>

    @Query("SELECT categoryName, SUM(amount) as totalAmount FROM expenses WHERE date BETWEEN :startDate AND :endDate GROUP BY categoryName")
    fun getCategorySummaries(startDate: String, endDate: String): LiveData<List<CategorySummary>>

    @Query("SELECT date, SUM(amount) as totalAmount FROM expenses GROUP BY date ORDER BY date ASC")
    fun getDailySpending(): LiveData<List<DailySpending>>
}

data class DailySpending(
    val date: String,
    val totalAmount: Double
)

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(goal: Goal)

    @Query("SELECT * FROM goals WHERE month = :month LIMIT 1")
    fun getGoalForMonth(month: String): LiveData<Goal?>
}

@Dao
interface SavingsGoalDao {
    @Insert
    suspend fun insert(goal: SavingsGoal)

    @Update
    suspend fun update(goal: SavingsGoal)

    @Delete
    suspend fun delete(goal: SavingsGoal)

    @Query("SELECT * FROM savings_goals")
    fun getAllSavingsGoals(): LiveData<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getSavingsGoalById(id: Long): SavingsGoal?
}

@Dao
interface RewardPointsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rewardPoints: RewardPoints)

    @Query("SELECT * FROM reward_points WHERE id = 1 LIMIT 1")
    fun getRewardPoints(): LiveData<RewardPoints?>

    @Query("SELECT * FROM reward_points WHERE id = 1 LIMIT 1")
    suspend fun getRewardPointsSync(): RewardPoints?
}

data class CategorySummary(
    val categoryName: String,
    val totalAmount: Double
)
