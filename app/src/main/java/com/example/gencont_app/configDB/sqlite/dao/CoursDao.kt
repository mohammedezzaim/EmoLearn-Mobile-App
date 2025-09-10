package com.example.gencont_app.configDB.sqlite.dao


import androidx.room.*
import com.example.gencont_app.configDB.sqlite.data.Cours


@Dao
interface CoursDao {
    // Méthodes de base
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(cours: Cours): Long

    @Query("SELECT * FROM Cours WHERE id = :id")
    suspend fun getCoursById(id: Long): Cours?

    @Update
    suspend fun update(cours: Cours)

    @Delete
    suspend fun delete(cours: Cours)

    @Query("SELECT * FROM Cours")
    suspend fun getAllCours(): List<Cours>

    // Méthodes supplémentaires
    @Query("SELECT * FROM Cours WHERE titre LIKE :titre")
    suspend fun searchCoursByTitre(titre: String): List<Cours>

    @Query("SELECT * FROM Cours WHERE utilisateurId = :utilisateurId")
    suspend fun getCoursByUtilisateur(utilisateurId: Long): List<Cours>

    @Query("SELECT * FROM Cours WHERE promptId = :promptId")
    suspend fun getCoursByPrompt(promptId: Long): List<Cours>

    @Query("SELECT COUNT(*) FROM Cours")
    suspend fun countAllCours(): Int

    @Query("DELETE FROM Cours")
    suspend fun deleteAllCours()

    @Query("SELECT * FROM Cours WHERE status_cours = :status")
    suspend fun getCoursByStatus(status: String): List<Cours>

    @Query("UPDATE Cours SET status_cours = :status WHERE id = :id")
    suspend fun updateCoursStatus(id: Long, status: String)

}