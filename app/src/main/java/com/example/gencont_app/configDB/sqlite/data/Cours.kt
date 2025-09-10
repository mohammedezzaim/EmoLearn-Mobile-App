package com.example.gencont_app.configDB.sqlite.data

import androidx.room.*

@Entity(
    tableName = "Cours",
    foreignKeys = [
        ForeignKey(
            entity = Prompt::class,
            parentColumns = ["id"],
            childColumns = ["promptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Utilisateur::class,
            parentColumns = ["id"],
            childColumns = ["utilisateurId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("promptId"),
        Index("utilisateurId")
    ]
)
data class Cours(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    val titre: String?,
    val description: String?,
    val nombreSection: Int?,
    @ColumnInfo(name = "status_cours") val statusCours: String?,
    @ColumnInfo(name = "urlImage") val urlImage: String,
    @ColumnInfo(name = "promptId") val promptId: Long?,
    @ColumnInfo(name = "utilisateurId") val utilisateurId: Long?
)
{
    // Nécessaire pour Firestore
    constructor() : this(0, null, null, null, null, "", null, null)

    // Méthode pour comparer les champs
    fun equalsIgnoreFields(other: Cours): Boolean {
        return this.id == other.id &&
                this.titre == other.titre &&
                this.description == other.description &&
                this.nombreSection == other.nombreSection &&
                this.statusCours == other.statusCours &&
                this.urlImage == other.urlImage &&
                this.promptId == other.promptId &&
                this.utilisateurId == other.utilisateurId
    }
}