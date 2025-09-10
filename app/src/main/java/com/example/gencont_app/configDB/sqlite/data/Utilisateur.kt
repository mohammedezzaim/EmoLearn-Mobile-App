package com.example.gencont_app.configDB.sqlite.data

import androidx.room.*

@Entity(tableName = "Utilisateur")
data class Utilisateur(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,

    @ColumnInfo(name = "nom") val nom: String?,

    @ColumnInfo(name = "prénom") val prénom: String?,

    @ColumnInfo(name = "email") val email: String?,

    @ColumnInfo(name = "mot_de_passe") val motDePasse: String?
) {
    constructor() : this(0, null, null, null, null)

    fun equalsIgnoreFields(other: Utilisateur): Boolean {
        return  this.id == other.id &&
                this.nom == other.nom &&
                this.prénom == other.prénom &&
                this.email == other.email &&
                this.motDePasse == other.motDePasse
    }
}