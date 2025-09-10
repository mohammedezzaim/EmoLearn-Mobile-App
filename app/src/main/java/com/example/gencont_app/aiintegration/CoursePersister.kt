import android.content.Context
import android.util.Log
import com.example.gencont_app.aiintegration.service_video.VideoSuggestionManager
import com.example.gencont_app.aiintegration.service_video.model.VideoQueryRequest
import com.example.gencont_app.configDB.firebase.repository.*
import com.example.gencont_app.configDB.sqlite.data.*
import com.example.gencont_app.configDB.sqlite.database.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.*

class CoursePersister(
    private val db: AppDatabase,
    private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun saveCourse(
        jsonResponse: String,
        userId: Long,
        promptStatus: String,
        langue: String
    ) = withContext(Dispatchers.IO) {
        // Nettoyage JSON
        val sanitized = jsonResponse.trim().let { raw ->
            if (raw.startsWith("```")) {
                raw.lines().drop(1).dropLast(1).joinToString("\n")
            } else raw
        }

        // Parsing JSON
        val root = JSONObject(sanitized).getJSONObject("cours")
        val titre = root.getString("titre")
        val niveau = root.optString("niveau")
        val desc = root.optString("description")
        val nSections = root.getInt("nombreSection")
        val urlImage = root.optString("urlImage", "")

        // Repositories
        val promptFirebaseRepository = PromptFirebaseRepository(db.promptDao(), firestore)
        val coursFirebaseRepository = CoursFirebaseRepository(db.coursDao(), firestore)
        val sectionFirebaseRepository = SectionFirebaseRepository(db.sectionDao(), firestore)
        val quizFirebaseRepository = QuizFirebaseRepository(db.quizDao(), firestore)
        val questionFirebaseRepository = QuestionFirebaseRepository(db.questionDao(), firestore)
        val reponseFirebaseRepository = ReponseFirebaseRepository(db.reponseDao(), firestore)

        // Sauvegarde Prompt
        val prompt = Prompt(
            Tags = null, coursName = titre, niveau = niveau,
            langue = langue, description = desc,
            status_user = promptStatus, utilisateurId = userId
        )
        val promptId = promptFirebaseRepository.insert(prompt)

        // Sauvegarde Cours
        val cours = Cours(
            titre = titre, description = desc, nombreSection = nSections,
            statusCours = "CREATED", urlImage = urlImage,
            promptId = promptId, utilisateurId = userId
        )
        val coursId = coursFirebaseRepository.insert(cours)

        // Liste temporaire pour mise à jour vidéos plus tard
        val createdSections = mutableListOf<Section>()

        // Parcours des sections
        val sections = root.getJSONArray("sections")
        for (i in 0 until sections.length()) {
            val secJson = sections.getJSONObject(i)

            val section = Section(
                titre = secJson.getString("titre"),
                urlImage = secJson.optString("urlImage", ""),
                urlVideo = secJson.optString("urlVideo", ""), // Peut être vide
                contenu = secJson.getString("contenu"),
                exemple = secJson.optString("exemple", ""),
                numeroOrder = i + 1,
                coursId = coursId
            )
            val sectionId = sectionFirebaseRepository.insert(section)

            // Récupérer la section avec ID
            val sectionWithId = section.copy(id = sectionId)
            createdSections.add(sectionWithId)

            // Quiz lié
            val quiz = Quiz(
                ref = UUID.randomUUID().toString(),
                lib = section.titre,
                nb_rep_correct = 1,
                score = 0.0,
                sectionId = sectionId
            )
            val quizId = quizFirebaseRepository.insert(quiz)

            // Questions et réponses
            if (secJson.has("quiz")) {
                val questions = secJson.getJSONArray("quiz")
                for (j in 0 until questions.length()) {
                    val qJson = questions.getJSONObject(j)
                    val question = Question(
                        ref = UUID.randomUUID().toString(),
                        libelle = qJson.getString("libelle"),
                        status_question = "NEW",
                        quizId = quizId
                    )
                    val questionId = questionFirebaseRepository.insert(question)

                    if (qJson.has("reponses")) {
                        val reps = qJson.getJSONArray("reponses")
                        for (k in 0 until reps.length()) {
                            val rJson = reps.getJSONObject(k)
                            val rep = Reponse(
                                ref = UUID.randomUUID().toString(),
                                lib = rJson.getString("lib"),
                                status = if (rJson.getBoolean("isCorrect")) "correct" else "incorrect",
                                questionId = questionId
                            )
                            reponseFirebaseRepository.insert(rep)
                        }
                    }
                }
            }
        }


//


        // Suggestion vidéos et mise à jour
        val coursEntity = db.coursDao().getCoursById(coursId)
        if (coursEntity != null) {
            val videoRequest = VideoQueryRequest(coursEntity, createdSections, context)
            val manager = VideoSuggestionManager()
            manager.suggestVideosPerSection(
                videoRequest,
                onSuccess = { results ->
                    CoroutineScope(Dispatchers.IO).launch {
                        results.forEach { (index, suggestedVideoUrl) ->
                            if (index in createdSections.indices) {
                                val sectionToUpdate = createdSections[index]
                                val videoUrl = extractVideoUrl(suggestedVideoUrl)
                                Log.d("VideoSuggestion", "Section ${sectionToUpdate.id} -> $videoUrl")

                                if (!videoUrl.isNullOrBlank()) {
                                    val updatedSection = sectionToUpdate.copy(urlVideo = videoUrl)
                                    sectionFirebaseRepository.updateById(sectionToUpdate.id, updatedSection)
                                }
                            }
                        }
                    }
                },
                onError = { error ->
                    Log.e("VideoSuggestionError", "Erreur lors de la suggestion : $error")
                }
            )
        }

//        val coursEntity = db.coursDao().getCoursById(coursId)
//        if (coursEntity != null) {
//            val videoRequest = VideoQueryRequest(coursEntity, createdSections, context)
//            val manager = VideoSuggestionManager()
//
//            manager.suggestVideo(
//                videoRequest,
//                onSuccess = { suggestedVideoUrl ->
//                    CoroutineScope(Dispatchers.IO).launch {
//                        val videoUrl = extractVideoUrl(suggestedVideoUrl)
//                        Log.d("VideoSuggestion", "Video URL suggérée : $videoUrl")
//
//                        // Mise à jour d'une seule section, par exemple la première
//                        if (!videoUrl.isNullOrBlank() && createdSections.isNotEmpty()) {
//                            val sectionToUpdate = createdSections.first() // ou un autre critère
//                            val updatedSection = sectionToUpdate.copy(urlVideo = videoUrl)
//                            sectionFirebaseRepository.updateById(sectionToUpdate.id, updatedSection)
//                        }
//                    }
//                },
//                onError = { error ->
//                    Log.e("VideoSuggestionError", "Erreur lors de la suggestion : $error")
//                }
//            )
//        }


    }

    fun extractVideoUrl(jsonString: String): String {
        return try {
            val jsonObject = JSONObject(jsonString)
            jsonObject.getString("video") // récupère la valeur du champ "video"
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}


//import com.example.gencont_app.configDB.database.AppDatabase
//import com.example.gencont_app.configDB.data.*
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import org.json.JSONObject
//import java.util.*
//
//class CoursePersister(private val db: AppDatabase) {
//
//    /**
//     * Extrait le JSON retourné par l'API et le persiste dans toutes les tables.
//     * @param jsonResponse la chaîne JSON brute contenant {"cours":{...}}
//     * @param userId       l'id de l'utilisateur qui a généré ce prompt
//     * @param promptStatus un status à stocker pour Prompt (ex: "DONE")
//     */
//    suspend fun saveCourse(
//        jsonResponse: String,
//        userId: Long,
//        promptStatus: String,
//        langue : String
//    ) = withContext(Dispatchers.IO) {
//        // 1) Sanitize : retirer fences Markdown si présentes
//        val sanitized = jsonResponse.trim().let { raw ->
//            if (raw.startsWith("```")) {
//                // supprime la première et la dernière ligne
//                raw
//                    .lines()
//                    .drop(1)
//                    .dropLast(1)
//                    .joinToString("\n")
//            } else {
//                raw
//            }
//        }
//        // 2) Construire le JSONObject sur le JSON pur
//        val root = JSONObject(sanitized).getJSONObject("cours")
//        val titre     = root.getString("titre")
//        val niveau    = root.optString("niveau")
//        val desc      = root.optString("description")
//        val nSections = root.getInt("nombreSection")
//
//        // 1) Sauvegarde du Prompt
//        val prompt = Prompt(
//            Tags       = null,
//            coursName  = titre,
//            niveau     = niveau,
//            langue     = langue,
//            description= desc,
//            status_user= promptStatus,
//            utilisateurId = userId
//        )
//        val promptId = db.promptDao().insert(prompt)
//
//        // 2) Sauvegarde du Cours
//        val cours = Cours(
//            titre         = titre,
//            description   = desc,
//            nombreSection = nSections,
//            statusCours   = "CREATED"
//        )
//        val coursId = db.coursDao().insert(cours)
//
//        // 3) Parcours des sections
//        val sections = root.getJSONArray("sections")
//        for (i in 0 until sections.length()) {
//            val secJson = sections.getJSONObject(i)
//            val section = Section(
//                titre       = secJson.getString("titre"),
//                contenu     = secJson.getString("contenu"),
//                numeroOrder = i + 1,
//                coursId     = coursId
//            )
//            val sectionId = db.sectionDao().insert(section)
//
//            // 4) Création du Quiz lié à cette section
//            val quiz = Quiz(
//                ref            = UUID.randomUUID().toString(),
//                lib            = section.titre,
//                nb_rep_correct = 1,
//                score          = 0.0
//            )
//            val quizId = db.quizDao().insert(quiz)
//
//            // 5) Lien Quiz ↔ Section
//            db.quizSectionDao().insert(
//                QuizSection(quizId = quizId, sectionId = sectionId)
//            )
//
//            // 6) Parcours des questions
//            val questions = secJson.getJSONArray("quiz")
//            for (j in 0 until questions.length()) {
//                val qJson = questions.getJSONObject(j)
//                // 6a) Insert Question
//                val question = Question(
//                    ref          = UUID.randomUUID().toString(),
//                    libelle      = qJson.getString("libelle"),
//                    status_question = "NEW"
//                )
//                val questionId = db.questionDao().insert(question)
//
//                // 6b) Lien Quiz ↔ Question
//                db.quizQuestionDao().insert(
//                    QuizQuestion(quizId = quizId, questionId = questionId)
//                )
//
//                // 6c) Parcours des réponses
//                val reps = qJson.getJSONArray("reponses")
//                for (k in 0 until reps.length()) {
//                    val rJson = reps.getJSONObject(k)
//                    val rep = Reponse(
//                        ref         = UUID.randomUUID().toString(),
//                        lib         = rJson.getString("lib"),
//                        status      = if (rJson.getBoolean("isCorrect")) "correct" else "incorrect",
//                        questionId  = questionId
//                    )
//                    db.reponseDao().insert(rep)
//                }
//            }
//        }
//    }
//}