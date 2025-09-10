package com.example.gencont_app.configDB.sqlite.data

import androidx.room.*

@Entity(
    tableName = "Quiz",
    foreignKeys = [ForeignKey(
        entity = Section::class,
        parentColumns = ["section_id"],
        childColumns = ["section_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("section_id")]
)
data class Quiz(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    val ref: String?,
    val lib: String?,
    val nb_rep_correct: Int?,
    val score: Double?,

    @ColumnInfo(name = "section_id")
    val sectionId: Long
) {
    companion object {
        const val TABLE_NAME = "Quiz"
    }

    // Constructeur par défaut
    constructor() : this(0, "", "", 0, 0.0, 0)

    // Methode pour comparer les champs
    fun equalsIgnoreFields(other: Quiz): Boolean {
        return this.id == other.id &&
                this.ref == other.ref &&
                this.lib == other.lib &&
                this.nb_rep_correct == other.nb_rep_correct &&
                this.score == other.score &&
                this.sectionId == other.sectionId
    }
}