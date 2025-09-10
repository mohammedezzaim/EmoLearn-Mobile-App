package com.example.gencont_app.configDB.sqlite.dao


import androidx.room.*
import com.example.gencont_app.configDB.sqlite.data.Prompt

@Dao
interface PromptDao {
    // Méthodes de base
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(prompt: Prompt): Long

    @Query("SELECT * FROM Prompt WHERE id = :id")
    suspend fun getPromptById(id: Long): Prompt?

    @Update
    suspend fun update(prompt: Prompt)

    @Delete
    suspend fun delete(prompt: Prompt)

    // Méthodes supplémentaires
    @Query("SELECT * FROM Prompt WHERE utilisateurId = :utilisateurId")
    suspend fun getPromptsByUtilisateur(utilisateurId: Long): List<Prompt>

    @Query("SELECT * FROM Prompt")
    suspend fun getAllPrompts(): List<Prompt>

    @Query("SELECT * FROM Prompt WHERE niveau = :niveau")
    suspend fun getPromptsByNiveau(niveau: String): List<Prompt>

    @Query("SELECT * FROM Prompt WHERE langue = :langue")
    suspend fun getPromptsByLangue(langue: String): List<Prompt>

    @Query("SELECT * FROM Prompt WHERE coursName LIKE :coursName")
    suspend fun searchPromptsByCoursName(coursName: String): List<Prompt>

    @Query("DELETE FROM Prompt")
    suspend fun deleteAllPrompts()

    @Query("SELECT COUNT(*) FROM Prompt")
    suspend fun countAllPrompts(): Int

    @Query("SELECT * FROM Prompt WHERE status_user = :status")
    suspend fun getPromptsByStatus(status: String): List<Prompt>

}