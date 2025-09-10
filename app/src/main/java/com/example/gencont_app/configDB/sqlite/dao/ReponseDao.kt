package com.example.gencont_app.configDB.sqlite.dao


import androidx.room.*
import com.example.gencont_app.configDB.sqlite.data.Reponse

@Dao
interface ReponseDao {
    // Méthodes de base
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(reponse: Reponse): Long

    @Query("SELECT * FROM Reponse WHERE id = :id")
    suspend fun getReponseById(id: Long): Reponse?

    @Update
    suspend fun update(reponse: Reponse)

    @Delete
    suspend fun delete(reponse: Reponse)

    // Méthodes supplémentaires
    @Query("SELECT * FROM Reponse WHERE questionId = :questionId")
    suspend fun getReponsesByQuestion(questionId: Long): List<Reponse>

    @Query("SELECT * FROM Reponse")
    suspend fun getAllReponses(): List<Reponse>

    @Query("SELECT * FROM Reponse WHERE status = :status")
    suspend fun getReponsesByStatus(status: String): List<Reponse>

    @Query("DELETE FROM Reponse WHERE questionId = :questionId")
    suspend fun deleteReponsesByQuestion(questionId: Long)

    @Query("SELECT COUNT(*) FROM Reponse WHERE questionId = :questionId")
    suspend fun countReponsesByQuestion(questionId: Long): Int

    @Query("DELETE FROM Reponse")
    suspend fun deleteAllReponses()

    @Query("UPDATE Reponse SET status = :status WHERE id = :reponseId")
    suspend fun updateReponseStatus(reponseId: Long, status: String)
}