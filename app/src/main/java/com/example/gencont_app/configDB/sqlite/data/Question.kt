package com.example.gencont_app.configDB.sqlite.data

import androidx.room.*

@Entity(
    tableName = "Question",
    foreignKeys = [ForeignKey(
        entity = Quiz::class,
        parentColumns = ["id"],
        childColumns = ["quiz_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("quiz_id")]
)
data class Question(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    val ref: String?,
    val libelle: String?,
    val status_question: String?,

    @ColumnInfo(name = "quiz_id")
    val quizId: Long
)
{
    companion object {
        const val TABLE_NAME = "Question"
    }

    // Constructeur par défaut
    constructor() : this(0, "", "", "", 0)

    // Methode pour comparer les champs
    fun equalsIgnoreFields(other: Question): Boolean {
        return this.id == other.id &&
                this.ref == other.ref &&
                this.libelle == other.libelle &&
                this.status_question == other.status_question &&
                this.quizId == other.quizId
    }
}