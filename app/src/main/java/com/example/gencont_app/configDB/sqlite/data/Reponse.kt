package com.example.gencont_app.configDB.sqlite.data

import androidx.room.*

@Entity(
    tableName = "Reponse",
    foreignKeys = [ForeignKey(entity = Question::class, parentColumns = ["id"], childColumns = ["questionId"])],
    indices = [Index("questionId")]
)
data class Reponse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ref: String?,
    val lib: String?,
    val status: String?,
    @ColumnInfo(name = "questionId") val questionId: Long?
)
{
    // Constructeur par défaut
    constructor() : this(0, null, null, null, null)

    // Méthode equalsIgnoreFields pour comparer tous les champs
    fun equalsIgnoreFields(other: Reponse): Boolean {
        return this.id == other.id &&
                this.ref == other.ref &&
                this.lib == other.lib &&
                this.status == other.status &&
                this.questionId == other.questionId
    }
}