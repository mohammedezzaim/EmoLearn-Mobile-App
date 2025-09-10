package com.example.gencont_app.aiintegration.service_video

import com.example.gencont_app.aiintegration.service_video.api.VideoSuggestionService
import com.example.gencont_app.aiintegration.service_video.model.VideoQueryRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException

class VideoSuggestionManager(
) {

    private val service = VideoSuggestionService()

    suspend fun suggestVideo(request: VideoQueryRequest, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val prompt = request.toPrompt()
        service.getVideoLink(prompt, onSuccess, onError)
    }


    suspend fun suggestVideosPerSection(
        request: VideoQueryRequest,
        onSuccess: (Map<Int, String>) -> Unit,
        onError: (String) -> Unit
    ) {
        val results = mutableMapOf<Int, String>()

        for ((index, section) in request.sections.withIndex()) {
            val prompt = request.toPromptForSection(section, index)

            val result = suspendCancellableCoroutine<String> { continuation ->
                service.getVideoLink(
                    prompt,
                    onResult = { content -> continuation.resume(content) {} },
                    onError = { error -> continuation.resumeWithException(Exception(error)) }
                )
            }

            results[index + 1] = result
        }

        onSuccess(results)
    }

}
