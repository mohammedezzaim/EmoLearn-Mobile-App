package com.example.gencont_app.aiintegration.service_video.model

data class ModelConfig(val model: String, val apiKey: String)

object ModelProvider {
    private val modelPool = listOf(
        ModelConfig("gpt-4o", "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"), // GPT-4o 1
        ModelConfig("gpt-4o", "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"), // GPT-4o 2
        ModelConfig("gpt-4o", "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"), // GPT-4o 3

//        ModelConfig("DeepSeek-V3-0324", "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"), // DeepSeek
//        ModelConfig("Llama-3.2-11B-Vision-Instruct", "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") // LLaMA
    )

    private var index = 0

    // Tourne en boucle sur les modèles
    fun getNextModel(): ModelConfig {
        val model = modelPool[index % modelPool.size]
        index++
        return model
    }
}
