package com.example.gencont_app.configDB.sqlite.dao

import androidx.room.*

import com.example.gencont_app.configDB.sqlite.data.Question
import androidx.room.Query

@Dao
interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(question: Question): Long

    @Query("SELECT * FROM Question WHERE id = :id")
    suspend fun getQuestionById(id: Long): Question?

    @Update
    suspend fun update(question: Question)

    @Delete
    suspend fun delete(question: Question)

    @Query("SELECT * FROM Question")
    suspend fun getAllQuestions(): List<Question>

    @Query("SELECT * FROM Question WHERE quiz_id = :quizId")
    suspend fun getQuestionsByQuiz(quizId: Long): List<Question>

    @Query("SELECT * FROM Question WHERE status_question = :status")
    suspend fun getQuestionsByStatus(status: String): List<Question>

    @Query("DELETE FROM Question WHERE quiz_id = :quizId")
    suspend fun deleteQuestionsByQuiz(quizId: Long)

    @Query("SELECT COUNT(*) FROM Question WHERE quiz_id = :quizId")
    suspend fun countQuestionsByQuiz(quizId: Long): Int

    @Query("DELETE FROM Question")
    suspend fun deleteAllQuestions()

    @Transaction
    @Query("SELECT * FROM Question WHERE quiz_id = :quizId ORDER BY id ASC")
    suspend fun getQuestionsWithReponses(quizId: Long): List<Question>
}