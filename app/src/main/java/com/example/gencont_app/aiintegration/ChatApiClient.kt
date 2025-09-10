package com.example.gencont_app.api

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object ChatApiClient {
    private const val TAG = "ChatAPI"
    private const val ENDPOINT = "https://models.inference.ai.azure.com/chat/completions"
//    private const val API_KEY = "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"  // Remplacez par votre clé deepseek
//    private const val API_KEY = "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"  // ezzaim
//    private const val API_KEY = "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"  // adel
//private const val API_KEY = "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"  // adel 2
//private const val API_KEY = "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"  // ismail 2
private const val API_KEY = "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"  // ezzaim 2
//private const val API_KEY = "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"  // chatgpt adel


    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * @param onResult callback déclenché avec le JSON de cours généré
     */
    fun generateCourseJson(
        titre: String,
        niveau: String,
        language: String,
        description: String,
        emotion: String,
        onResult: (jsonCourse: String) -> Unit
    ) {
        val systemPrompt = """
    Tu es un assistant pédagogique expert dans la génération de cours structurés au format JSON. Chaque section doit contenir au moins 150 mots pour assurer une couverture approfondie du sujet.
    Tu reçois en user prompt :
      • un titre de cours
      • un niveau parmi {débutant, intermédiaire, avancé}
      • la language de contenu 
      • une courte description
      • une émotion parmi {happy, neutral, sad, excited, anxious}

    Selon l’`emotion` :
      – Adapte la **tonalité** :
          • happy/excited → chaleureux, encourageant, dynamique  
          • neutral       → clair, factuel, équilibré  
          • sad/anxious   → empathique, rassurant, apaisant  
      – Détermine le **nombre de sections** :
          • happy/excited → 3 sections rythmées (
          • neutral       → 2 sections de longueur standard 
          • sad/anxious   → 1 sections concises, calmes 

    Chaque section doit contenir :
      1. **titre**  
      2. **contenu** explicatif adapté au niveau et à l’émotion  
      3. **exemple** concret  
      4. **quiz** de 4 questions, chacune avec 3 réponses (`lib`) dont 1 seule a `"isCorrect": true`.

   format de ce JSON :
    {
      "cours": {
        "titre": "...",
        "niveau": "...",
        "nombreSection": N,
        "sections": [
          {
            "titre": "...",
            "contenu": "...",
            "exemple": "...",
            "quiz": [
              {
                "libelle": "Question 1 …",
                "reponses": [
                  { "lib": "Réponse A", "isCorrect": false },
                  { "lib": "Réponse B", "isCorrect": true  },
                  { "lib": "Réponse C", "isCorrect": false }
                ]
              },
              { "libelle": "Question 2 …", "reponses": [ { … } ] },
              { "libelle": "Question 3 …", "reponses": [ { … } ] },
              { "libelle": "Question 4 …", "reponses": [ { … } ] }
            ]
          }
          // … autres sections
        ]
      }
    }
""".trimIndent()


        val userPayload = """
        {
          "titre": "${titre.replace("\"","\\\"")}",
          "niveau": "${niveau.replace("\"","\\\"")}",
          "language": "${language.replace("\"","\\\"")}",
          "description": "${description.replace("\"","\\\"")}",
          "emotion": "${emotion.replace("\"","\\\"")}"
        }
        Instruction :
        Génère un JSON unique avec ce schéma :
        {
          "cours": { … }
        }
        """.trimIndent()


        // Dans generateCourseJson(), remplacez la construction du payload par :
        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPayload)
            })
        }
        // Construction du JSON complet
        var chatgpt = "gpt-4.1-nano"
        var deepseek = "DeepSeek-V3-0324"
        val fullJson = JSONObject().apply {
            put("model", chatgpt)
            put("temperature", 0.6)
            put("top_p", 0.7)
            put("max_tokens", 11000)
            put("messages", messagesArray)    // <— utilise maintenant un JSONArray
        }.toString(2)

        Log.d(TAG, "Payload envoyé :\n$fullJson")

        val requestBody = fullJson
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("Authorization", "Bearer $API_KEY")
            .post(requestBody)
            .build()

  /*      client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Erreur réseau : ${e.message}", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    // ② Affiche le code ET tout le corps d'erreur
                    Log.e(TAG, "HTTP ${response.code} - body:\n$bodyString")
                    return
                }

                try {
                    val content = JSONObject(bodyString)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    Log.d(TAG, "Cours JSON généré :\n$content")
                } catch (e: Exception) {
                    Log.e(TAG, "Parsing échoué : ${e.message}", e)
                    Log.d(TAG, "Réponse brute :\n$bodyString")
                }
            }
        })*/
        // … construction du fullJson, envoi de la requête …
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Erreur réseau : ${e.message}", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP ${response.code} - body:\n$bodyString")
                    return
                }
                try {
                    val content = JSONObject(bodyString)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    // **ici**, on renvoie la chaîne JSON complète de "cours"
                    onResult(content)
                } catch (e: Exception) {
                    Log.e(TAG, "Parsing échoué : ${e.message}", e)
                    Log.d(TAG, "Réponse brute :\n$bodyString")
                }
            }
        })
    }
}