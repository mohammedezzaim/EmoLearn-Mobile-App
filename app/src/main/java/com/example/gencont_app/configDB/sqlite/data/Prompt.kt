package com.example.gencont_app.configDB.sqlite.data

import androidx.room.*

@Entity(
    tableName = "Prompt",
    foreignKeys = [ForeignKey(entity = Utilisateur::class, parentColumns = ["id"], childColumns = ["utilisateurId"])],
    indices = [Index("utilisateurId")]
)
data class Prompt(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,

    @TypeConverters(Converters::class)
    val Tags: List<String>?,
    val coursName: String?,
    val niveau: String?,
    val langue: String?,
    val description: String?,
    val status_user: String?,

    @ColumnInfo(name = "utilisateurId") val utilisateurId: Long?
)
{
    companion object {
        const val TABLE_NAME = "Prompt"
    }

    // Constructeur par défaut
    constructor() : this(0, emptyList(), "", "", "", "","", 0)

    // Méthode pour comparer les champs
    fun equalsIgnoreFields(other: Prompt): Boolean {
        return this.id == other.id &&
                this.Tags == other.Tags &&
                this.coursName == other.coursName &&
                this.niveau == other.niveau &&
                this.langue == other.langue &&
                this.description == other.description &&
                this.status_user == other.status_user &&
                this.utilisateurId == other.utilisateurId
    }
}