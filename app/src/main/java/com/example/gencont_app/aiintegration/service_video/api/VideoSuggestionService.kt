package com.example.gencont_app.aiintegration.service_video.api

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class VideoSuggestionService(
) {
    private val client = OkHttpClient()

    // Endpoint
    private val endpoint = "https://models.inference.ai.azure.com/chat/completions"   // Mets ici ton endpoint Azure

    //model
    private val gpt_4o = "gpt-4o"
    private val llama3211B_Vision_Instruct  = "Llama-3.2-11B-Vision-Instruct"
//    private val DeepSeek_V3_0324 = "DeepSeek-V3-0324"

//    private val DeepSeek_R1 = "DeepSeek-R1"
//    private val apikey =


    // Nom du déploiement dans Azure
//    private val apiKey = "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" // gpt_40 1
//    private val apiKey = "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" // gpt_40 1
//    private val apiKey = "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" // gpt_40 1
//    private val apiKey = "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" // DeepSeek-V3-0324
//    private val apiKey = "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" // llama3211B_Vision_Instruct


    var chatgpt = "gpt-4.1-nano"
//    private val apiKey = "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"  // ismail 2
//    private val apiKey = "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"  // ismail 2




//    private var apiKey = "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" // gpt 4o
    private var apiKey = "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" // gpt 4o

//    private var apiKey = "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" // nano

    fun getVideoLink(prompt: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        // Construction JSON messages
        val messages = JSONArray().apply {
            put(JSONObject().put("role", "user").put("content", prompt))
        }

        val json = JSONObject()
            .put("model", gpt_4o)
            .put("messages", messages)
            .put("temperature", 0.8)
            .put("top_p", 0.8)
            .put("max_tokens", 1000)  // 10k est trop élevé, Azure limite souvent ~4k

        val body = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey") // Azure OpenAI utilise ce header
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Erreur réseau")
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string()
                if (!response.isSuccessful || bodyString == null) {
                    onError("Erreur : ${response.code}")
                    return
                }
                try {
                    val resJson = JSONObject(bodyString)
                    val result = resJson
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    onResult(result.trim())
                } catch (e: Exception) {
                    onError("Erreur parsing : ${e.message}")
                }
            }
        })
    }
}
