package com.example.gencont_app.configDB.firebase.syncData

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.gencont_app.configDB.firebase.repository.*
import com.example.gencont_app.configDB.sqlite.data.Utilisateur
import com.example.gencont_app.configDB.sqlite.data.*
import com.example.gencont_app.configDB.sqlite.database.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

object SyncDataDebug {

    fun syncFromFirebaseToLocal(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val firestore = FirebaseFirestore.getInstance()

            try {
                val utilisateurRepo = UtilisateurFirebaseRepository(db.utilisateurDao(), firestore)
                val coursRepo = CoursFirebaseRepository(db.coursDao(), firestore)
                val promptRepo = PromptFirebaseRepository(db.promptDao(), firestore)
                val sectionRepo = SectionFirebaseRepository(db.sectionDao(), firestore)
                val quizRepo = QuizFirebaseRepository(db.quizDao(), firestore)
                val questionRepo = QuestionFirebaseRepository(db.questionDao(), firestore)
                val reponseRepo = ReponseFirebaseRepository(db.reponseDao(), firestore)

                // Vérification directe des changements pour chaque repository
                val hasChanges = utilisateurRepo.syncFromFirebaseToLocal() ||
                        promptRepo.syncFromFirebaseToLocal() ||
                        coursRepo.syncFromFirebaseToLocal() ||
                        sectionRepo.syncFromFirebaseToLocal() ||
                        quizRepo.syncFromFirebaseToLocal() ||
                        questionRepo.syncFromFirebaseToLocal() ||
                        reponseRepo.syncFromFirebaseToLocal()

                withContext(Dispatchers.Main) {
                    if (hasChanges) {
                        Toast.makeText(context, "Données importées depuis Firebase avec succès", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("SyncFirebaseToLocal", "Aucun changement détecté")                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("SyncFirebaseToLocal", "Erreur lors de la synchronisation Firebase → Locale ${e.message}", e)
                    Toast.makeText(
                        context,
                        "Erreur d'importation Firebase → Locale : ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    fun syncFromLocalToFirebase(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val firestore = FirebaseFirestore.getInstance()

            try {
                val utilisateurRepo = UtilisateurFirebaseRepository(db.utilisateurDao(), firestore)
                val coursRepo = CoursFirebaseRepository(db.coursDao(), firestore)
                val promptRepo = PromptFirebaseRepository(db.promptDao(), firestore)
                val sectionRepo = SectionFirebaseRepository(db.sectionDao(), firestore)
                val quizRepo = QuizFirebaseRepository(db.quizDao(), firestore)
                val questionRepo = QuestionFirebaseRepository(db.questionDao(), firestore)
                val reponseRepo = ReponseFirebaseRepository(db.reponseDao(), firestore)

                utilisateurRepo.syncFromLocalToFirebase()
                promptRepo.syncFromLocalToFirebase()
                coursRepo.syncFromLocalToFirebase()
                sectionRepo.syncFromLocalToFirebase()
                quizRepo.syncFromLocalToFirebase()
                questionRepo.syncFromLocalToFirebase()
                reponseRepo.syncFromLocalToFirebase()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Données exportées vers Firebase avec succès", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erreur d'exportation Locale → Firebase : ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}