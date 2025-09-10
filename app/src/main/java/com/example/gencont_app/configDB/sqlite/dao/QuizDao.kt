package com.example.gencont_app.configDB.sqlite.dao


import androidx.room.*
import com.example.gencont_app.configDB.sqlite.data.Cours
import com.example.gencont_app.configDB.sqlite.data.Quiz

@Dao
interface QuizDao {
    // Insert - Create
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quiz: Quiz): Long

    // Read
    @Query("SELECT * FROM Quiz WHERE id = :id LIMIT 1")
    suspend fun getQuizById(id: Long): Quiz?

    @Query("SELECT * FROM Quiz WHERE section_id = :sectionId LIMIT 1")
    suspend fun getQuizBySection(sectionId: Long): Quiz?

    // Update
    @Update
    suspend fun update(quiz: Quiz)

    @Query("UPDATE Quiz SET score = :score WHERE id = :quizId")
    suspend fun updateScore(quizId: Long, score: Double)

    // Delete
    @Delete
    suspend fun delete(quiz: Quiz)

    @Query("DELETE FROM Quiz WHERE section_id = :sectionId")
    suspend fun deleteBySection(sectionId: Long)

    // Utilitaires
    @Query("SELECT EXISTS(SELECT 1 FROM Quiz WHERE section_id = :sectionId LIMIT 1)")
    suspend fun existsForSection(sectionId: Long): Boolean

    @Query("SELECT score FROM Quiz WHERE section_id = :sectionId")
    suspend fun getScoreForSection(sectionId: Long): Double?

    @Query("DELETE FROM Quiz")
    suspend fun deleteAllQuizzes()

    @Query("SELECT * FROM Quiz")
    suspend fun getAllQuizzes(): List<Quiz>

}