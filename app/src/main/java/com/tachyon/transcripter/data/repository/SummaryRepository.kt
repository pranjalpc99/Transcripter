package com.tachyon.transcripter.data.repository

import android.util.Log
import com.google.gson.Gson
import com.tachyon.transcripter.BuildConfig
import com.tachyon.transcripter.data.local.dao.SummaryDao
import com.tachyon.transcripter.data.local.dao.TranscriptSegmentDao
import com.tachyon.transcripter.data.local.entity.GenerationStatus
import com.tachyon.transcripter.data.local.entity.Summary
import com.tachyon.transcripter.data.remote.api.GeminiApi
import com.tachyon.transcripter.data.remote.dto.SummaryRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

// Create request data classes
data class GeminiRequest(
    val contents: List<ContentRequest>,
    val generationConfig: GenerationConfig
)

data class ContentRequest(
    val parts: List<PartRequest>
)

data class PartRequest(
    val text: String
)

data class GenerationConfig(
    val temperature: Float,
    val maxOutputTokens: Int,
    val responseMimeType: String
)

/**
 * Repository for managing summary generation and storage.
 * Handles LLM API calls with streaming support.
 */
@Singleton
class SummaryRepository @Inject constructor(
    private val geminiApi: GeminiApi,
    private val summaryDao: SummaryDao,
    private val transcriptSegmentDao: TranscriptSegmentDao,
    private val transcriptionRepository: TranscriptionRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // ========== Summary Generation ==========

    /**
     * Generate a summary for a session with streaming support.
     * Emits partial results as they arrive from the API.
     *
     * @param sessionId Session ID to generate summary for
     * @return Flow of SummaryStreamState
     */
    fun generateSummary(sessionId: String): Flow<SummaryStreamState> = flow {
        try {
            emit(SummaryStreamState.Loading)

            // Get full transcript
            val segments = transcriptSegmentDao.getSegmentsBySession(sessionId)
            val transcript = segments.joinToString(" ") { it.text }

            if (transcript.isBlank()) {
                emit(SummaryStreamState.Error("No transcript available. Please transcribe audio first."))
                return@flow
            }

            Log.d(TAG, "Generating summary for ${transcript.length} characters")

            // Create or get existing summary entity
            var summary = summaryDao.getBySessionId(sessionId) ?: Summary(
                sessionId = sessionId,
                title = null,
                summary = null,
                actionItems = null,
                keyPoints = null,
                generationStatus = GenerationStatus.GENERATING
            )
            summaryDao.insert(summary)

            // Build prompt
            val prompt = buildSummaryPrompt(transcript)

            // Create API request
            val request = GeminiRequest(
                contents = listOf(
                    ContentRequest(
                        parts = listOf(PartRequest(text = prompt))
                    )
                ),
                generationConfig = GenerationConfig(
                    temperature = TEMPERATURE,
                    maxOutputTokens = MAX_TOKENS,
                    responseMimeType = "application/json"
                )
            )

            val gson = Gson()
            val requestJson = gson.toJson(request)

            val requestBody = requestJson.toRequestBody("application/json".toMediaType())

            Log.d(TAG, "Calling Gemini API for summary generation...")

            // Call API (non-streaming for now, can add streaming later)
            val response = geminiApi.generateContent(getApiKey(), requestBody)

            if (response.isSuccessful) {
                val geminiResponse = response.body()
                val content = geminiResponse
                    ?.candidates?.firstOrNull()
                    ?.content?.parts?.firstOrNull()
                    ?.text

                Log.d(TAG, "Summary response received: ${content?.take(100)}...")

                if (content != null) {
                    // Parse content
                    val parsed = parseSummaryContent(content)

                    // Update summary with final content
                    summary = summary.copy(
                        title = parsed.title,
                        summary = parsed.summary,
                        actionItems = parsed.actionItems.joinToString("\n"),
                        keyPoints = parsed.keyPoints.joinToString("\n"),
                        generationStatus = GenerationStatus.COMPLETED,
                        partialContent = null,
                        updatedAt = System.currentTimeMillis()
                    )

                    summaryDao.update(summary)

                    Log.d(TAG, "Summary saved successfully")

                    // Emit completion state
                    emit(SummaryStreamState.Complete(summary))
                } else {
                    throw Exception("No content in response")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                throw Exception("API Error [${response.code()}]: $errorBody")
            }

        } catch (e: Exception) {
            // Update database with error
            try {
                summaryDao.getBySessionId(sessionId)?.let { existingSummary ->
                    summaryDao.update(
                        existingSummary.copy(
                            generationStatus = GenerationStatus.FAILED,
                            errorMessage = e.message?.take(500),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            } catch (dbError: Exception) {
                Log.e(TAG, "Failed to update error in database", dbError)
            }

            emit(SummaryStreamState.Error(e.message ?: "Unknown error occurred"))
        }
    }.catch { e ->
        emit(SummaryStreamState.Error(e.message ?: "Stream error"))
    }.flowOn(ioDispatcher)

    /**
     * Generate summary without streaming (for background processing).
     *
     * @param sessionId Session ID
     * @return Result with generated Summary
     */
    suspend fun generateSummaryNonStreaming(sessionId: String): Result<Summary> =
        withContext(ioDispatcher) {
            try {
                val segments = transcriptSegmentDao.getSegmentsBySession(sessionId)
                val transcript = segments.joinToString(" ") { it.text }

                if (transcript.isBlank()) {
                    return@withContext Result.failure(
                        Exception("No transcript available")
                    )
                }

                // Create or get summary entity
                var summary = summaryDao.getBySessionId(sessionId) ?: Summary(
                    sessionId = sessionId,
                    title = null,
                    summary = null,
                    actionItems = null,
                    keyPoints = null,
                    generationStatus = GenerationStatus.GENERATING
                )
                summaryDao.insert(summary)

                // Build prompt
                val prompt = buildSummaryPrompt(transcript)

                // Create API request
                // Escape the prompt for JSON
                val escapedPrompt = prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")

                // Create API request
                val requestJson = """
                {
                  "contents": [{
                    "parts": [{
                      "text": "$escapedPrompt"
                    }]
                  }],
                  "generationConfig": {
                    "temperature": $TEMPERATURE,
                    "maxOutputTokens": $MAX_TOKENS,
                    "responseMimeType": "application/json"
                  }
                }
                """.trimIndent()

                val requestBody = requestJson.toRequestBody("application/json".toMediaType())

                // Call API
                val response = geminiApi.generateContent(getApiKey(), requestBody)

                if (!response.isSuccessful) {
                    throw ApiException(
                        code = response.code(),
                        message = response.errorBody()?.string() ?: "Unknown error"
                    )
                }

                val geminiResponse = response.body()
                    ?: throw Exception("Empty response from API")

                val content = geminiResponse.candidates?.firstOrNull()
                    ?.content?.parts?.firstOrNull()
                    ?.text
                    ?: throw Exception("No content in response")

                // Parse content
                val parsed = parseSummaryContent(content)

                // Update summary
                summary = summary.copy(
                    title = parsed.title,
                    summary = parsed.summary,
                    actionItems = parsed.actionItems.joinToString("\n"),
                    keyPoints = parsed.keyPoints.joinToString("\n"),
                    generationStatus = GenerationStatus.COMPLETED,
                    updatedAt = System.currentTimeMillis()
                )

                summaryDao.update(summary)

                Result.success(summary)

            } catch (e: Exception) {
                // Update database with error
                summaryDao.getBySessionId(sessionId)?.let { existingSummary ->
                    summaryDao.update(
                        existingSummary.copy(
                            generationStatus = GenerationStatus.FAILED,
                            errorMessage = e.message?.take(500),
                            attemptCount = existingSummary.attemptCount + 1,
                            lastAttempt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }

                Result.failure(e)
            }
        }

    /**
     * Build the prompt for summary generation.
     */
    private fun buildSummaryPrompt(transcript: String): String {
        return """
            You are an AI assistant that creates structured summaries of voice recordings.
            
            Based on the following transcript, generate a structured summary in JSON format with these sections:
            
            1. **title**: A brief, descriptive title (max 10 words)
            2. **summary**: A concise summary of the main content (2-3 sentences)
            3. **actionItems**: List of actionable tasks or decisions mentioned (array of strings)
            4. **keyPoints**: Main topics or important points discussed (array of strings)
            
            **IMPORTANT**: Return ONLY valid JSON in this exact format:
            {
              "title": "Brief Title Here",
              "summary": "Concise summary of the recording in 2-3 sentences.",
              "actionItems": [
                "First action item or task",
                "Second action item or task"
              ],
              "keyPoints": [
                "First key point discussed",
                "Second key point discussed"
              ]
            }
            
            If no action items are mentioned, return an empty array for actionItems.
            If the transcript is unclear or empty, provide a general summary.
            
            Transcript:
            ${transcript.take(MAX_TRANSCRIPT_LENGTH)}
        """.trimIndent()
    }

    /**
     * Parse the LLM response into structured summary data.
     */
    private fun parseSummaryContent(content: String): ParsedSummary {
        return try {
            // Try to extract JSON from the content
            val jsonContent = extractJsonFromContent(content)
            val json = JSONObject(jsonContent)

            ParsedSummary(
                title = json.optString("title", "Summary").take(100),
                summary = json.optString("summary", "").take(1000),
                actionItems = json.optJSONArray("actionItems")?.toStringList() ?: emptyList(),
                keyPoints = json.optJSONArray("keyPoints")?.toStringList() ?: emptyList()
            )
        } catch (e: JSONException) {
            // Fallback: parse as plain text
            parsePlainTextSummary(content)
        }
    }

    /**
     * Extract JSON from content that might have surrounding text.
     */
    private fun extractJsonFromContent(content: String): String {
        // Try to find JSON object boundaries
        val startIndex = content.indexOf('{')
        val endIndex = content.lastIndexOf('}')

        return if (startIndex >= 0 && endIndex > startIndex) {
            content.substring(startIndex, endIndex + 1)
        } else {
            content
        }
    }

    /**
     * Parse plain text summary when JSON parsing fails.
     */
    private fun parsePlainTextSummary(content: String): ParsedSummary {
        // Try to extract sections from plain text
        val lines = content.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        var title = "Summary"
        var summary = ""
        val actionItems = mutableListOf<String>()
        val keyPoints = mutableListOf<String>()

        var currentSection = ""

        for (line in lines) {
            when {
                line.contains("title:", ignoreCase = true) -> {
                    title = line.substringAfter(":", "").trim()
                    currentSection = "title"
                }
                line.contains("summary:", ignoreCase = true) -> {
                    summary = line.substringAfter(":", "").trim()
                    currentSection = "summary"
                }
                line.contains("action", ignoreCase = true) -> {
                    currentSection = "actions"
                }
                line.contains("key point", ignoreCase = true) -> {
                    currentSection = "keypoints"
                }
                line.startsWith("-") || line.startsWith("•") || line.matches(Regex("^\\d+\\..*")) -> {
                    val item = line.removePrefix("-").removePrefix("•")
                        .replaceFirst(Regex("^\\d+\\."), "").trim()
                    when (currentSection) {
                        "actions" -> actionItems.add(item)
                        "keypoints" -> keyPoints.add(item)
                    }
                }
                currentSection == "summary" && summary.isNotEmpty() -> {
                    summary += " $line"
                }
            }
        }

        // If no structured data found, use first 200 chars as summary
        if (summary.isEmpty() && content.length > 200) {
            summary = content.take(200) + "..."
        } else if (summary.isEmpty()) {
            summary = content
        }

        return ParsedSummary(
            title = title,
            summary = summary.take(1000),
            actionItems = actionItems,
            keyPoints = keyPoints
        )
    }

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    /**
     * Convert JSONArray to List<String>.
     */
    private fun JSONArray.toStringList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            list.add(getString(i))
        }
        return list
    }

    // ========== Summary Retrieval ==========

    /**
     * Get summary by session ID.
     */
    suspend fun getSummaryBySessionId(sessionId: String): Summary? =
        withContext(ioDispatcher) {
            summaryDao.getBySessionId(sessionId)
        }

    /**
     * Observe summary (reactive).
     */
    fun observeSummary(sessionId: String): Flow<Summary?> {
        return summaryDao.observeBySessionId(sessionId)
    }

    /**
     * Get formatted summary as plain text.
     */
    suspend fun getFormattedSummary(sessionId: String): String? =
        withContext(ioDispatcher) {
            val summary = summaryDao.getBySessionId(sessionId) ?: return@withContext null

            buildString {
                summary.title?.let {
                    appendLine("# $it")
                    appendLine()
                }

                summary.summary?.let {
                    appendLine("## Summary")
                    appendLine(it)
                    appendLine()
                }

                summary.actionItems?.let { items ->
                    if (items.isNotBlank()) {
                        appendLine("## Action Items")
                        items.split("\n").forEach { item ->
                            appendLine("- $item")
                        }
                        appendLine()
                    }
                }

                summary.keyPoints?.let { points ->
                    if (points.isNotBlank()) {
                        appendLine("## Key Points")
                        points.split("\n").forEach { point ->
                            appendLine("- $point")
                        }
                    }
                }
            }
        }

    // ========== Retry Operations ==========

    /**
     * Retry summary generation for a failed attempt.
     */
    suspend fun retrySummaryGeneration(sessionId: String): Flow<SummaryStreamState> {
        return flow {
            // Reset status to pending
            summaryDao.getBySessionId(sessionId)?.let { existingSummary ->
                summaryDao.update(
                    existingSummary.copy(
                        generationStatus = GenerationStatus.PENDING,
                        errorMessage = null,
                        partialContent = null
                    )
                )
            }

            // Generate again
            generateSummary(sessionId).collect { state ->
                emit(state)
            }
        }.flowOn(ioDispatcher)
    }

    // ========== Update Operations ==========

    /**
     * Update summary manually (user edits).
     */
    suspend fun updateSummary(summary: Summary): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                summaryDao.update(
                    summary.copy(updatedAt = System.currentTimeMillis())
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Update partial content during streaming.
     */
    suspend fun updatePartialContent(sessionId: String, content: String) =
        withContext(ioDispatcher) {
            summaryDao.updatePartialContent(sessionId, content)
        }

    // ========== Deletion ==========

    /**
     * Delete summary for a session.
     */
    suspend fun deleteSummary(sessionId: String): Result<Unit> =
        withContext(ioDispatcher) {
            try {
                summaryDao.deleteBySession(sessionId)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ========== Export ==========

    /**
     * Export summary as JSON.
     */
    suspend fun exportSummaryAsJson(sessionId: String): Result<String> =
        withContext(ioDispatcher) {
            try {
                val summary = summaryDao.getBySessionId(sessionId)
                    ?: return@withContext Result.failure(Exception("Summary not found"))

                val json = JSONObject().apply {
                    put("sessionId", summary.sessionId)
                    put("title", summary.title ?: "")
                    put("summary", summary.summary ?: "")

                    val actionItemsArray = JSONArray()
                    summary.actionItems?.split("\n")?.forEach { item ->
                        if (item.isNotBlank()) actionItemsArray.put(item)
                    }
                    put("actionItems", actionItemsArray)

                    val keyPointsArray = JSONArray()
                    summary.keyPoints?.split("\n")?.forEach { point ->
                        if (point.isNotBlank()) keyPointsArray.put(point)
                    }
                    put("keyPoints", keyPointsArray)

                    put("createdAt", summary.createdAt)
                    put("updatedAt", summary.updatedAt)
                }

                Result.success(json.toString(2)) // Pretty print with indent of 2

            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Export summary as Markdown.
     */
    suspend fun exportSummaryAsMarkdown(sessionId: String): Result<String> =
        withContext(ioDispatcher) {
            try {
                val formatted = getFormattedSummary(sessionId)
                    ?: return@withContext Result.failure(Exception("Summary not found"))

                Result.success(formatted)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    companion object {
        private const val TAG = "SummaryRepository"
        private const val MAX_TOKENS = 1000
        private const val TEMPERATURE = 0.7f
        private const val MAX_TRANSCRIPT_LENGTH = 50000 // ~50k characters
    }
}

/**
 * Sealed class for summary generation states.
 */
sealed class SummaryStreamState {
    object Loading : SummaryStreamState()
    data class Streaming(val partialContent: String) : SummaryStreamState()
    data class Complete(val summary: Summary) : SummaryStreamState()
    data class Error(val message: String) : SummaryStreamState()
}

/**
 * Parsed summary data.
 */
data class ParsedSummary(
    val title: String,
    val summary: String,
    val actionItems: List<String>,
    val keyPoints: List<String>
)