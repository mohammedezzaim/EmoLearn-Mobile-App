package com.example.gencont_app.configDB.sqlite.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gencont_app.configDB.sqlite.dao.*
import com.example.gencont_app.configDB.sqlite.data.*

@Database(
    entities = [
        Cours::class,
        Prompt::class,
        Question::class,
        Quiz::class,
        Reponse::class,
        Section::class,
        Utilisateur::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coursDao(): CoursDao
    abstract fun promptDao(): PromptDao
    abstract fun questionDao(): QuestionDao
    abstract fun quizDao(): QuizDao
    abstract fun reponseDao(): ReponseDao
    abstract fun sectionDao(): SectionDao
    abstract fun utilisateurDao(): UtilisateurDao

    companion object {
        private const val DATABASE_NAME = "gencont_db"

        // Singleton pattern to ensure only one instance of the database is created
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration() // ONLY FOR TESTING! Remove in production
                    .build()
                    .also { instance = it }
            }
        }
    }
}