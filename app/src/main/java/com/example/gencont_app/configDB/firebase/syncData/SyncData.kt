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

object SyncData {
    fun populateDatabase(db: AppDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            // 1. Créer 10 utilisateurs
            val userIds = mutableListOf<Long>()
           /* for (i in 1..10) {
                val user = Utilisateur(
                    nom = "User$i",
                    prénom = "Name$i",
                    email = "user$i@example.com",
                    motDePasse = "password$i"
                )
                userIds.add(db.utilisateurDao().insert(user))
            }*/

            // 2. Créer 10 prompts
            val promptIds = mutableListOf<Long>()
           /* for (i in 1..10) {
                val prompt = Prompt(
                    Tags = listOf("tag${i}", "tag${i+1}"),
                    coursName = "Cours $i",
                    niveau = when (i % 3) {
                        0 -> "Débutant"
                        1 -> "Intermédiaire"
                        else -> "Avancé"
                    },
                    langue = if (i % 2 == 0) "Français" else "Anglais",
                    description = "Description du prompt $i",
                    status_user = if (i % 2 == 0) "ACTIF" else "INACTIF",
                    utilisateurId = userIds[i-1]
                )
                promptIds.add(db.promptDao().insert(prompt))
            }*/

            // 3. Créer 10 cours
            val coursIds = mutableListOf<Long>()
          /*  for (i in 1..10) {
                val cours = Cours(
                    titre = "Cours $i",
                    description = "Description du cours $i",
                    nombreSection = 3,
                    statusCours = when (i % 3) {
                        0 -> "CREATED"
                        1 -> "PUBLISHED"
                        else -> "ARCHIVED"
                    },
                    urlImage = "https://example.com/image$i.jpg",
                    promptId = promptIds[i-1],
                    utilisateurId = userIds[i-1]
                )
                coursIds.add(db.coursDao().insert(cours))
            }*/

            // 4. Créer 10 sections par cours (3 sections par cours)
            val sectionIds = mutableListOf<Long>()
           /* for (coursId in coursIds) {
                for (j in 1..3) {
                    val section = Section(
                        titre = "Section $j",
                        urlImage = "https://example.com/section$j.jpg",
                        urlVideo = "https://example.com/video$j.mp4",
                        contenu = "Contenu de la section $j",
                        exemple = "Exemple pour la section $j",
                        numeroOrder = j,
                        coursId = coursId
                    )
                    sectionIds.add(db.sectionDao().insert(section))
                }
            }*/

            // 5. Créer 10 quiz (1 par section)
            val quizIds = mutableListOf<Long>()
           /* for ((index, sectionId) in sectionIds.withIndex()) {
                val quiz = Quiz(
                    ref = UUID.randomUUID().toString(),
                    lib = "Quiz pour section $sectionId",
                    nb_rep_correct = (index % 4) + 1,
                    score = index * 10.0,
                    sectionId = sectionId
                )
                quizIds.add(db.quizDao().insert(quiz))
            }*/

            // 6. Créer 10 questions par quiz (3 questions par quiz)
            val questionIds = mutableListOf<Long>()
           /* for ((quizIndex, quizId) in quizIds.withIndex()) {
                for (j in 1..3) {
                    val question = Question(
                        ref = UUID.randomUUID().toString(),
                        libelle = "Question $j pour quiz $quizId",
                        status_question = if (j % 2 == 0) "ACTIVE" else "INACTIVE",
                        quizId = quizId
                    )
                    questionIds.add(db.questionDao().insert(question))
                }
            }*/

            // 7. Créer 10 réponses par question (4 réponses par question)
            /*for ((questionIndex, questionId) in questionIds.withIndex()) {
                for (k in 1..4) {
                    val reponse = Reponse(
                        ref = UUID.randomUUID().toString(),
                        lib = "Réponse $k pour question $questionId",
                        status = if (k == 1) "correct" else "incorrect",
                        questionId = questionId
                    )
                    db.reponseDao().insert(reponse)
                }
            }*/
        }
    }

    suspend fun clearDatabase(db: AppDatabase) {
        with(db) {

            reponseDao().deleteAllReponses()
            questionDao().deleteAllQuestions()
            quizDao().deleteAllQuizzes()
            sectionDao().deleteAllSections()
            coursDao().deleteAllCours()
            promptDao().deleteAllPrompts()
            utilisateurDao().deleteAllUtilisateurs()
        }
    }


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