package com.example.gencont_app.configDB.sqlite.dao

import androidx.room.*
import com.example.gencont_app.configDB.sqlite.data.Cours
import com.example.gencont_app.configDB.sqlite.data.Section

@Dao
interface SectionDao {
    // INSERT
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(section: Section): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(sections: List<Section>): List<Long>

    // SELECT
    @Query("SELECT * FROM sections WHERE section_id = :id LIMIT 1")
    suspend fun getById(id: Long): Section?

    @Query("SELECT * FROM sections WHERE cours_id = :coursId ORDER BY numero_order ASC")
    suspend fun getSectionsForCours(coursId: Long): List<Section>

    @Transaction
    @Query("SELECT * FROM sections WHERE cours_id = :coursId ORDER BY numero_order ASC")
    suspend fun getByCoursId(coursId: Long): List<Section>

    @Query("SELECT COUNT(*) FROM sections WHERE cours_id = :coursId")
    suspend fun countByCoursId(coursId: Long): Int

    // UPDATE
    @Update
    suspend fun update(section: Section): Int

    @Query("UPDATE sections SET numero_order = :newOrder WHERE section_id = :sectionId")
    suspend fun updateOrder(sectionId: Long, newOrder: Int): Int

    // DELETE
    @Delete
    suspend fun delete(section: Section): Int

    @Query("DELETE FROM sections WHERE section_id = :sectionId")
    suspend fun deleteById(sectionId: Long): Int

    @Query("DELETE FROM sections WHERE cours_id = :coursId")
    suspend fun deleteByCoursId(coursId: Long): Int


    // Ajouts
    @Query("SELECT MAX(numero_order) FROM sections WHERE cours_id = :coursId")
    suspend fun getMaxOrderNumber(coursId: Long): Int?

    @Query("UPDATE sections SET numero_order = numero_order + 1 WHERE cours_id = :coursId AND numero_order >= :startOrder")
    suspend fun incrementOrderNumbers(coursId: Long, startOrder: Int)

    @Query("SELECT * FROM sections WHERE cours_id = :coursId AND numero_order = :orderNumber")
    suspend fun getSectionByOrder(coursId: Long, orderNumber: Int): Section?

    @Query("UPDATE sections SET titre = :newTitle WHERE section_id = :sectionId")
    suspend fun updateSectionTitle(sectionId: Long, newTitle: String)

    @Query("UPDATE sections SET contenu = :newContent WHERE section_id = :sectionId")
    suspend fun updateSectionContent(sectionId: Long, newContent: String)

    @Query("DELETE FROM sections")
    suspend fun deleteAllSections()

    @Query("Select * FROM sections")
    suspend fun getAllSections(): List<Section>

    // SELECT
    @Query("SELECT * FROM sections WHERE section_id = :id LIMIT 1")
    suspend fun getSectionById(id: Long): Section?

}

