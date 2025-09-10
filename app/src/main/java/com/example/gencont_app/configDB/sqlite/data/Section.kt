package com.example.gencont_app.configDB.sqlite.data

import androidx.room.*

@Entity(
    tableName = "sections",
    foreignKeys = [ForeignKey(
        entity = Cours::class,
        parentColumns = ["id"],
        childColumns = ["cours_id"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )],
    indices = [Index("cours_id")]
)
data class Section(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "section_id")
    var id: Long = 0,

    @ColumnInfo(name = "titre")
    val titre: String,

    @ColumnInfo(name = "urlImage")
    val urlImage: String,

    @ColumnInfo(name = "urlVideo")
    val urlVideo: String,

    @ColumnInfo(name = "contenu")
    val contenu: String,

    @ColumnInfo(name = "exemple")
    val exemple: String,

    @ColumnInfo(name = "numero_order")
    val numeroOrder: Int,

    @ColumnInfo(name = "cours_id")
    val coursId: Long,

    @ColumnInfo(name = "date_creation", defaultValue = "CURRENT_TIMESTAMP")
    val dateCreation: String? = null
) {
    // Constructeur par défaut
    constructor() : this(0, "", "", "", "", "", 0, 0, null)

    companion object {
        const val TABLE_NAME = "sections"
    }

    fun equalsIgnoreFields(other: Section): Boolean {
        return  this.id == other.id &&
                this.titre == other.titre &&
                this.urlImage == other.urlImage &&
                this.urlVideo == other.urlVideo &&
                this.contenu == other.contenu &&
                this.exemple == other.exemple &&
                this.numeroOrder == other.numeroOrder &&
                this.coursId == other.coursId &&
                this.dateCreation == other.dateCreation
    }

}
