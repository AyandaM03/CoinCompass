package com.example.coincompass.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User)

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?
}

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories")
    fun getAllCategories(): LiveData<List<Category>>
}

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: Expense)

    @Query("SELECT * FROM expenses")
    fun getAllExpenses(): LiveData<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    fun getExpensesBetweenDates(startDate: String, endDate: String): LiveData<List<Expense>>

    @Query("SELECT categoryName, SUM(amount) as totalAmount FROM expenses WHERE date BETWEEN :startDate AND :endDate GROUP BY categoryName")
    fun getCategorySummaries(startDate: String, endDate: String): LiveData<List<CategorySummary>>
}

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(goal: Goal)

    @Query("SELECT * FROM goals WHERE month = :month LIMIT 1")
    fun getGoalForMonth(month: String): LiveData<Goal?>
}

data class CategorySummary(
    val categoryName: String,
    val totalAmount: Double
)
