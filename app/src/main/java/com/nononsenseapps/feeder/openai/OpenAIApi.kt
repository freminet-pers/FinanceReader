package com.nononsenseapps.feeder.openai

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatResponseFormat
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.chat.TextContent
import com.aallam.openai.api.exception.OpenAIAPIException
import com.aallam.openai.api.exception.OpenAITimeoutException
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAIHost
import com.nononsenseapps.feeder.archmodel.OpenAISettings
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class OpenAIApi(
    private val appLang: String,
    private val openAIClientFactory: (OpenAISettings) -> OpenAIClient,
) {
    /** 系统语言的 ISO 639-1 代码（如 zh/en），用于总结返回首行的 Lang 标记（须为 ASCII）。 */
    private val appLangCode: String = Locale.getDefault().language

    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    @Serializable
    data class SummaryResponse(
        val lang: String,
        val content: String,
    )

    @Serializable
    private data class DeepLTranslateRequest(
        val text: List<String>,
        val target_lang: String,
        val tag_handling: String? = null,
        val split_sentences: String = "nonewlines",
        val preserve_formatting: Boolean = true,
    )

    @Serializable
    private data class DeepLTranslation(
        val detected_source_language: String,
        val text: String,
    )

    @Serializable
    private data class DeepLTranslateResponse(
        val translations: List<DeepLTranslation>,
    )

    /** Qwen-MT 翻译模型（OpenAI 兼容模式）的消息体：只接受 user 角色，不接受 system。 */
    @Serializable
    private data class QwenMtMessage(
        val role: String,
        val content: String,
    )

    /** Qwen-MT 的 translation_options：语言用英文全称（如 "Chinese"），源语言可用 "auto"。 */
    @Serializable
    private data class QwenMtTranslationOptions(
        @SerialName("source_lang") val sourceLang: String,
        @SerialName("target_lang") val targetLang: String,
        @SerialName("domains") val domains: String? = null,
    )

    @Serializable
    private data class QwenMtChatRequest(
        val model: String,
        val messages: List<QwenMtMessage>,
        @SerialName("translation_options") val translationOptions: QwenMtTranslationOptions,
    )

    @Serializable
    private data class QwenMtChatResponse(
        val choices: List<QwenMtChoice> = emptyList(),
    )

    @Serializable
    private data class QwenMtChoice(
        val message: QwenMtMessage? = null,
    )

    sealed interface SummaryResult {
        val content: String

        data class Success(
            val id: String,
            val created: Long,
            val model: String,
            override val content: String,
            val promptTokens: Int,
            val completeTokens: Int,
            val totalTokens: Int,
            val detectedLanguage: String,
        ) : SummaryResult

        data class Error(
            override val content: String,
        ) : SummaryResult
    }

    sealed interface TranslationResult {
        val content: String

        data class Success(
            override val content: String,
            val detectedLanguage: String,
        ) : TranslationResult

        data class Error(
            override val content: String,
            val action: ErrorAction = ErrorAction.None,
        ) : TranslationResult

        enum class ErrorAction {
            None,
            OpenSystemTranslationSettings,
        }
    }

    sealed interface ModelsResult {
        data object MissingToken : ModelsResult

        data object AzureApiVersionRequired : ModelsResult

        data object AzureDeploymentIdRequired : ModelsResult

        data class Success(
            val ids: List<String>,
        ) : ModelsResult

        data class Error(
            val message: String?,
        ) : ModelsResult
    }

    companion object {
        private val LANG_REGEX = Regex("^Lang: \"?([a-zA-Z_-]+)\"?$")
    }

    private fun okHttpClient(timeoutSeconds: Int): OkHttpClient =
        OkHttpClient
            .Builder()
            .callTimeout(timeoutSeconds.coerceIn(30, 600).toLong(), TimeUnit.SECONDS)
            .build()

    suspend fun listModelIds(settings: OpenAISettings): ModelsResult {
        if (settings.isLocalTranslation) {
            return ModelsResult.Success(ids = emptyList())
        }
        if (settings.key.isEmpty()) {
            return ModelsResult.MissingToken
        }
        if (settings.isDeepL) {
            return verifyDeepLSettings(settings)
        }
        if (settings.isPerplexity) {
            return ModelsResult.Success(ids = emptyList())
        }
        if (settings.isAzure) {
            if (settings.azureApiVersion.isBlank()) {
                return ModelsResult.AzureApiVersionRequired
            }
            if (settings.azureDeploymentId.isBlank()) {
                return ModelsResult.AzureDeploymentIdRequired
            }
        }
        return try {
            openAIClientFactory(settings)
                .models()
                .sortedByDescending { it.created }
                .map { it.id.id }
                .let { ModelsResult.Success(ids = it) }
        } catch (e: Exception) {
            ModelsResult.Error(message = e.message ?: e.cause?.message)
        }
    }

    private fun verifyDeepLSettings(settings: OpenAISettings): ModelsResult =
        runCatching {
            postJson<DeepLTranslateRequest, DeepLTranslateResponse>(
                settings = settings,
                url = settings.toDeepLTranslateUrl(),
                headers = mapOf("Authorization" to "DeepL-Auth-Key ${settings.key}"),
                requestBody =
                    DeepLTranslateRequest(
                        text = listOf("Hello"),
                        target_lang = "DE",
                    ),
                failurePrefix = "DeepL verification failed",
            )
        }.fold(
            onSuccess = { response ->
                if (response.translations.isEmpty()) {
                    ModelsResult.Error(message = "DeepL verification failed: no translation was returned")
                } else {
                    ModelsResult.Success(ids = emptyList())
                }
            },
            onFailure = { ModelsResult.Error(message = it.messageOrCause()) },
        )

    suspend fun summarize(
        content: String,
        settings: OpenAISettings,
    ): SummaryResult {
        if (settings.isDeepL) {
            return SummaryResult.Error(content = "Summarization is not supported for this translation-only provider")
        }
        if (settings.isLocalTranslation) {
            return SummaryResult.Error(content = "Summarization is not supported for this translation-only provider")
        }
        try {
            val response =
                openAIClientFactory(settings).chatCompletion(
                    request = summaryRequest(content, settings),
                    requestOptions = null,
                )
            val summaryResponse: SummaryResponse =
                parseSummaryResponse(
                    response.choices
                        .firstOrNull()
                        ?.message
                        ?.content ?: throw IllegalStateException("Response content is null"),
                )
            return SummaryResult.Success(
                id = response.id,
                model = response.model.id,
                content = summaryResponse.content,
                created = response.created,
                promptTokens = response.usage?.promptTokens ?: 0,
                completeTokens = response.usage?.completionTokens ?: 0,
                totalTokens = response.usage?.totalTokens ?: 0,
                detectedLanguage = summaryResponse.lang,
            )
        } catch (e: Exception) {
            return SummaryResult.Error(content = e.message ?: e.cause?.message ?: "")
        }
    }

    suspend fun translate(
        content: String,
        targetLanguage: String,
        settings: OpenAISettings,
        preserveHtml: Boolean = false,
        systemPrompt: String = "",
        sourceLangHint: String = "",
    ): TranslationResult {
        if (settings.isDeepL) {
            return translateWithDeepL(
                settings = settings,
                content = content,
                targetLanguage = targetLanguage,
                preserveHtml = preserveHtml,
            )
        }
        if (settings.isLocalTranslation) {
            return TranslationResult.Error(content = "Local translation is not available through this API")
        }
        if (settings.isQwenMtModel) {
            return translateWithQwenMt(
                content = content,
                targetLanguage = targetLanguage,
                settings = settings,
                preserveHtml = preserveHtml,
                sourceLangHint = sourceLangHint,
            )
        }
        return translateWithOpenAI(
            content = content,
            targetLanguage = targetLanguage,
            settings = settings,
            preserveHtml = preserveHtml,
            systemPrompt = systemPrompt,
            sourceLangHint = sourceLangHint,
        )
    }

    /** 以最小 chat 请求验证配置可用性（兼容端点可能不支持 /models 列表）。 */
    suspend fun testConnection(settings: OpenAISettings): TranslationResult {
        if (settings.isDeepL) {
            return when (val result = listModelIds(settings)) {
                is ModelsResult.Success -> TranslationResult.Success(content = "ok", detectedLanguage = "")
                is ModelsResult.MissingToken -> TranslationResult.Error(content = "Missing token")
                is ModelsResult.AzureApiVersionRequired -> TranslationResult.Error(content = "Azure API version required")
                is ModelsResult.AzureDeploymentIdRequired -> TranslationResult.Error(content = "Azure deployment ID required")
                is ModelsResult.Error -> TranslationResult.Error(content = result.message ?: "DeepL verification failed")
            }
        }
        if (settings.isLocalTranslation) {
            return TranslationResult.Error(content = "Local translation needs no connection test")
        }
        if (settings.isQwenMtModel) {
            return translateQwenMtChunk(
                content = "ping",
                sourceLanguage = "English",
                targetLanguage = "Chinese",
                settings = settings,
            )
        }
        return translateWithOpenAI(
            content = "ping",
            targetLanguage = "en",
            settings = settings,
            preserveHtml = false,
            systemPrompt = TEST_CONNECTION_SYSTEM_PROMPT,
            sourceLangHint = "en",
        )
    }

    private suspend fun translateWithOpenAI(
        content: String,
        targetLanguage: String,
        settings: OpenAISettings,
        preserveHtml: Boolean,
        systemPrompt: String,
        sourceLangHint: String,
    ): TranslationResult {
        // 运行时兜底：非本地明文 http 一律拒绝（防 API Key 明文外泄）
        if (settings.baseUrl.isInsecureNonLocalUrl()) {
            return TranslationResult.Error(content = "The endpoint must use https://")
        }

        // 长文按块级边界分块，串行翻译后合并（控制单请求 token 与失败面）
        val chunks = chunkTranslationContent(content = content, preserveHtml = preserveHtml)
        val translatedChunks = ArrayList<String>(chunks.size)
        for (chunk in chunks) {
            when (val result = translateChunkWithRetry(chunk, targetLanguage, settings, preserveHtml, systemPrompt, sourceLangHint)) {
                is TranslationResult.Success -> translatedChunks += result.content
                is TranslationResult.Error -> return result
            }
        }
        return TranslationResult.Success(
            content = translatedChunks.joinToString(separator = if (preserveHtml) "\n" else "\n\n"),
            detectedLanguage = "",
        )
    }

    /**
     * Qwen-MT 翻译模型专用路径。
     *
     * Qwen-MT（qwen-mt-flash / qwen-mt-plus / qwen-mt-turbo）是「机器翻译专用模型」，与通用 chat 模型有两点关键差异：
     * 1. messages 只接受 `user` 角色，不接受 `system`（否则报 "Role must be in [user, assistant]"）；
     * 2. 目标语言 / 源语言通过 `translation_options.source_lang` / `target_lang` 指定（英文全称，源语言可 "auto"），
     *    而不是写在系统提示词里。
     *
     * 因此这里绕过 openai-kotlin 的 ChatCompletionRequest（它无 extra_body 字段），直接发 JSON。
     */
    private suspend fun translateWithQwenMt(
        content: String,
        targetLanguage: String,
        settings: OpenAISettings,
        preserveHtml: Boolean,
        sourceLangHint: String,
    ): TranslationResult {
        // 运行时兜底：非本地明文 http 一律拒绝（防 API Key 明文外泄）
        if (settings.baseUrl.isInsecureNonLocalUrl()) {
            return TranslationResult.Error(content = "The endpoint must use https://")
        }

        // Qwen-MT 是纯文本翻译模型，不按 HTML 标签语义翻译；preserveHtml 时剥掉标签，译完按段落回填 <p>
        val (translatableText, isHtml) =
            if (preserveHtml) {
                htmlToTranslatableText(content) to true
            } else {
                content to false
            }

        val chunks = chunkTranslationContent(content = translatableText, preserveHtml = false)
        val translatedChunks = ArrayList<String>(chunks.size)
        for (chunk in chunks) {
            when (
                val result =
                    translateQwenMtChunk(
                        content = chunk,
                        sourceLanguage = toQwenMtLanguageName(sourceLangHint) ?: "auto",
                        targetLanguage =
                            toQwenMtLanguageName(targetLanguage)
                                ?.takeUnless { it == "auto" }
                                ?: "Chinese",
                        settings = settings,
                    )
            ) {
                is TranslationResult.Success -> translatedChunks += result.content
                is TranslationResult.Error -> return result
            }
        }
        val joined = translatedChunks.joinToString(separator = "\n\n")
        return TranslationResult.Success(
            content = if (isHtml) wrapPlainTextInParagraphs(joined) else joined,
            detectedLanguage = "",
        )
    }

    /** 单块 Qwen-MT 翻译（带瞬时错误重试，4xx 快速失败）。 */
    private suspend fun translateQwenMtChunk(
        content: String,
        sourceLanguage: String,
        targetLanguage: String,
        settings: OpenAISettings,
    ): TranslationResult {
        var lastError = "Translation failed"
        for (attempt in 0 until MAX_TRANSLATION_ATTEMPTS) {
            try {
                val response =
                    postJson<QwenMtChatRequest, QwenMtChatResponse>(
                        settings = settings,
                        url = settings.baseUrl.trimEnd('/') + "/chat/completions",
                        headers = mapOf("Authorization" to "Bearer ${settings.key}"),
                        requestBody =
                            QwenMtChatRequest(
                                model = settings.modelId,
                                messages =
                                    listOf(
                                        QwenMtMessage(
                                            role = "user",
                                            content = content,
                                        ),
                                    ),
                                translationOptions =
                                    QwenMtTranslationOptions(
                                        sourceLang = sourceLanguage,
                                        targetLang = targetLanguage,
                                        domains = QWEN_MT_FINANCE_DOMAINS,
                                    ),
                            ),
                        failurePrefix = "Translation request failed",
                    )
                val text =
                    response.choices
                        .firstOrNull()
                        ?.message
                        ?.content
                        ?.trim()
                        .orEmpty()
                if (text.isNotBlank()) {
                    return TranslationResult.Success(content = text, detectedLanguage = "")
                }
                lastError = "Response content is null"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 保留上次错误，避免 message 为 null 时把错误信息覆盖成空字符串
                val message = e.messageOrCause().orEmpty()
                if (message.isNotBlank()) {
                    lastError = message
                }
                // postJson 对非 2xx 抛 IllegalStateException（含 HTTP code）；4xx 为配置错误，快速失败不重试
                val statusCode = extractHttpStatusCode(lastError)
                if (statusCode != null && statusCode in 400..499 && statusCode != 429) {
                    return TranslationResult.Error(content = lastError)
                }
            }
            if (attempt < MAX_TRANSLATION_ATTEMPTS - 1) {
                delay(RETRY_BACKOFF_MS * (attempt + 1L))
            }
        }
        return TranslationResult.Error(content = lastError)
    }

    private suspend fun translateChunkWithRetry(
        content: String,
        targetLanguage: String,
        settings: OpenAISettings,
        preserveHtml: Boolean,
        systemPrompt: String,
        sourceLangHint: String,
    ): TranslationResult {
        var lastError = "Translation failed"
        for (attempt in 0 until MAX_TRANSLATION_ATTEMPTS) {
            try {
                val response =
                    openAIClientFactory(settings).chatCompletion(
                        request =
                            translationRequest(
                                content = content,
                                targetLanguage = targetLanguage,
                                settings = settings,
                                preserveHtml = preserveHtml,
                                systemPrompt = systemPrompt,
                                sourceLangHint = sourceLangHint,
                            ),
                        requestOptions = null,
                    )
                val text =
                    response.choices
                        .firstOrNull()
                        ?.message
                        ?.content
                        ?.trim()
                        .orEmpty()
                if (text.isNotBlank()) {
                    // 源语言由本机语言识别管线判定（DeepL 路径才使用其返回值）
                    return TranslationResult.Success(content = text, detectedLanguage = "")
                }
                lastError = "Response content is null"
            } catch (e: CancellationException) {
                throw e
            } catch (e: OpenAIAPIException) {
                // 4xx（除 429）为配置错误：快速失败，不重试
                lastError = e.message ?: "HTTP ${e.statusCode}"
                if (e.statusCode !in RETRYABLE_STATUS_CODES) {
                    return TranslationResult.Error(content = lastError)
                }
            } catch (e: Exception) {
                // 超时/IO 等瞬时错误可重试；其余（如本地逻辑错误）快速失败
                if (e !is OpenAITimeoutException && e !is IOException && e !is HttpRequestTimeoutException) {
                    return TranslationResult.Error(content = e.messageOrCause().orEmpty())
                }
                lastError = e.messageOrCause().orEmpty()
            }
            if (attempt < MAX_TRANSLATION_ATTEMPTS - 1) {
                delay(RETRY_BACKOFF_MS * (attempt + 1L))
            }
        }
        return TranslationResult.Error(content = lastError)
    }

    private fun translationRequest(
        content: String,
        targetLanguage: String,
        settings: OpenAISettings,
        preserveHtml: Boolean,
        systemPrompt: String,
        sourceLangHint: String,
    ): ChatCompletionRequest =
        ChatCompletionRequest(
            model = ModelId(id = settings.modelId),
            messages =
                listOf(
                    ChatMessage(
                        role = ChatRole.System,
                        messageContent =
                            TextContent(
                                buildString {
                                    val effectivePrompt =
                                        systemPrompt
                                            .ifBlank { DEFAULT_TRANSLATION_SYSTEM_PROMPT }
                                    append(
                                        effectivePrompt
                                            .replace("{target_language}", targetLanguage.trim())
                                            .replace(
                                                "{source_language}",
                                                sourceLangHint
                                                    .trim()
                                                    .ifBlank { "the original language" },
                                            ),
                                    )
                                    if (preserveHtml) {
                                        append(
                                            "\n\nThe input contains HTML markup. " +
                                                "Preserve all HTML tags and their structure exactly; " +
                                                "translate only the visible text content.",
                                        )
                                    }
                                },
                            ),
                    ),
                    ChatMessage(
                        role = ChatRole.User,
                        messageContent = TextContent(content),
                    ),
                ),
            responseFormat = ChatResponseFormat.Text,
        )

    private fun translateWithDeepL(
        settings: OpenAISettings,
        content: String,
        targetLanguage: String,
        preserveHtml: Boolean,
    ): TranslationResult {
        val targetLanguageCode = targetLanguage.toDeepLTargetLanguageCode()
        return runCatching {
            postJson<DeepLTranslateRequest, DeepLTranslateResponse>(
                settings = settings,
                url = settings.toDeepLTranslateUrl(),
                headers = mapOf("Authorization" to "DeepL-Auth-Key ${settings.key}"),
                requestBody =
                    DeepLTranslateRequest(
                        text = listOf(content),
                        target_lang = targetLanguageCode,
                        tag_handling = if (preserveHtml) "html" else null,
                    ),
                failurePrefix = "DeepL request failed",
            ).translations.firstOrNull()
                ?: throw IllegalStateException("DeepL returned no translations")
        }.fold(
            onSuccess = { translation ->
                staticTranslationSuccess(
                    content = translation.text,
                    detectedLanguage = translation.detected_source_language,
                )
            },
            onFailure = { TranslationResult.Error(content = it.messageOrCause().orEmpty()) },
        )
    }

    private fun parseSummaryResponse(content: String): SummaryResponse {
        val firstLine = content.lineSequence().firstOrNull() ?: ""
        val result = LANG_REGEX.find(firstLine)
        return SummaryResponse(
            lang = result?.groupValues?.getOrNull(1) ?: "",
            content = content.replaceFirst(firstLine, "").trim(),
        )
    }

    private fun summaryRequest(
        content: String,
        settings: OpenAISettings,
    ): ChatCompletionRequest =
        ChatCompletionRequest(
            model = ModelId(id = settings.modelId),
            messages =
                listOf(
                    ChatMessage(
                        role = ChatRole.System,
                        messageContent =
                            TextContent(
                                listOf(
                                    "You are an assistant in an RSS reader app, summarizing article content.",
                                    "The app language is '$appLang'. Always write the summary in this language, regardless of the article's language.",
                                    "First line must be exactly: 'Lang: \"$appLangCode\"' with NO markdown formatting around the Lang line whatsoever.",
                                    "Keep summaries up to 100 words, 3 paragraphs, with up to 3 bullet points per paragraph.",
                                    "For readability use markdown formatting: **bold** for emphasis, *italics* for quotes, bullet points (-) for lists, # headers for sections, and > for block quotes.",
                                    "Use markdown to structure content and improve readability.",
                                    "Use only single language.",
                                    "Keep full quotes if any.",
                                ).joinToString(separator = " "),
                            ),
                    ),
                    ChatMessage(
                        role = ChatRole.User,
                        messageContent = TextContent("Summarize:\n\n$content"),
                    ),
                ),
            responseFormat = ChatResponseFormat.Text,
        )

    private inline fun <reified RequestBodyT, reified ResponseBodyT> postJson(
        settings: OpenAISettings,
        url: String,
        requestBody: RequestBodyT,
        failurePrefix: String,
        headers: Map<String, String> = emptyMap(),
    ): ResponseBodyT {
        val request =
            Request
                .Builder()
                .url(url)
                .apply {
                    header("Content-Type", "application/json")
                    headers.forEach { (name, value) -> header(name, value) }
                }.post(
                    json
                        .encodeToString(requestBody)
                        .toRequestBody("application/json".toMediaType()),
                ).build()

        return okHttpClient(settings.timeoutSeconds)
            .newCall(request)
            .execute()
            .use { response ->
                if (!response.isSuccessful) {
                    throw httpFailure(failurePrefix, response)
                }

                json.decodeFromString<ResponseBodyT>(
                    response.body.string(),
                )
            }
    }

    private fun staticTranslationSuccess(
        content: String,
        detectedLanguage: String,
    ): TranslationResult.Success =
        TranslationResult.Success(
            content = content,
            detectedLanguage = detectedLanguage,
        )

    private fun httpFailure(
        prefix: String,
        response: Response,
    ): IllegalStateException =
        IllegalStateException(
            "$prefix: HTTP ${response.code}${response.message.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""}",
        )

    private fun Throwable.messageOrCause(): String? = message ?: cause?.message
}

val OpenAISettings.isQwenMtModel: Boolean
    get() =
        modelId
            .trim()
            .lowercase()
            .let { id ->
                id.startsWith("qwen-mt") ||
                    id.startsWith("mt-") ||
                    id.contains("qwen-mt")
            }

val OpenAISettings.isAzure: Boolean
    get() = baseUrl.contains("openai.azure.com", ignoreCase = true)

val OpenAISettings.isPerplexity: Boolean
    get() = baseUrl.contains("api.perplexity.ai", ignoreCase = true)

val OpenAISettings.isDeepL: Boolean
    get() = baseUrl.contains("deepl.com", ignoreCase = true)

val OpenAISettings.isLocalTranslation: Boolean
    get() = baseUrl == LOCAL_TRANSLATION_PROVIDER_URL

val OpenAISettings.isValid: Boolean
    get() =
        if (isLocalTranslation) {
            true
        } else if (isDeepL) {
            key.isNotEmpty()
        } else {
            modelId.isNotEmpty() &&
                key.isNotEmpty() &&
                if (isAzure) azureApiVersion.isNotBlank() && azureDeploymentId.isNotBlank() else true
        }

val OpenAISettings.canSummarize: Boolean
    get() = isValid && !isDeepL && !isLocalTranslation

val OpenAISettings.canTranslate: Boolean
    get() = isValid

val OpenAISettings.canUseAsTranslationApi: Boolean
    get() = canTranslate

val OpenAISettings.isBlankConfiguration: Boolean
    get() =
        key.isBlank() &&
            modelId.isBlank() &&
            baseUrl.isBlank() &&
            azureApiVersion.isBlank() &&
            azureDeploymentId.isBlank()

fun OpenAISettings.toOpenAIHost(withAzureDeploymentId: Boolean): OpenAIHost =
    baseUrl.let { baseUrl ->
        when {
            baseUrl.isEmpty() -> OpenAIHost.OpenAI

            // Azure 的路径规范要求 /openai 段（可选 /deployments/<id>）；路径必须以 / 结尾
            isAzure ->
                OpenAIHost(
                    baseUrl =
                        URLBuilder()
                            .takeFrom(baseUrl)
                            .also {
                                it.appendPathSegments("openai")
                                if (withAzureDeploymentId && azureDeploymentId.isNotBlank()) {
                                    it.appendPathSegments("deployments", azureDeploymentId)
                                }
                            }.buildString()
                            .let { if (it.endsWith("/")) it else "$it/" },
                    queryParams =
                        azureApiVersion.let { apiVersion ->
                            if (apiVersion.isEmpty()) emptyMap() else mapOf("api-version" to apiVersion)
                        },
                )

            // 其余 OpenAI 兼容端点（DeepSeek/Kimi/智谱/通义/Perplexity/自定义）：
            // 地址原样使用；openai-kotlin 约定：含路径时必须以 "/" 结尾
            else ->
                OpenAIHost(
                    baseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/",
                    queryParams = emptyMap(),
                )
        }
    }

fun OpenAISettings.toDeepLTranslateUrl(): String =
    URLBuilder()
        .takeFrom(normalizedDeepLBaseUrl())
        .also {
            it.appendPathSegments("v2", "translate")
        }.buildString()

fun OpenAIHost.toUrl(): URLBuilder =
    URLBuilder()
        .takeFrom(baseUrl)
        .also {
            queryParams.forEach { (k, v) -> it.parameters.append(k, v) }
        }

private fun String.toDeepLTargetLanguageCode(): String =
    trim()
        .uppercase()
        .replace('-', '_')
        .let { normalized ->
            when (normalized) {
                "ENGLISH", "EN" -> "EN"
                "EN_GB", "ENGLISH_UK", "ENGLISH_GB", "BRITISH_ENGLISH" -> "EN-GB"
                "EN_US", "ENGLISH_US", "AMERICAN_ENGLISH" -> "EN-US"
                "GERMAN", "DE" -> "DE"
                "FRENCH", "FR" -> "FR"
                "SPANISH", "ES" -> "ES"
                "PORTUGUESE", "PT" -> "PT"
                "PT_BR", "PORTUGUESE_BR", "BRAZILIAN_PORTUGUESE" -> "PT-BR"
                "PT_PT", "PORTUGUESE_PT", "EUROPEAN_PORTUGUESE" -> "PT-PT"
                "ITALIAN", "IT" -> "IT"
                "DUTCH", "NL" -> "NL"
                "POLISH", "PL" -> "PL"
                "RUSSIAN", "RU" -> "RU"
                "JAPANESE", "JA" -> "JA"
                "CHINESE", "ZH" -> "ZH"
                "CZECH", "CS" -> "CS"
                "DANISH", "DA" -> "DA"
                "GREEK", "EL" -> "EL"
                "FINNISH", "FI" -> "FI"
                "HUNGARIAN", "HU" -> "HU"
                "INDONESIAN", "ID" -> "ID"
                "KOREAN", "KO" -> "KO"
                "LITHUANIAN", "LT" -> "LT"
                "LATVIAN", "LV" -> "LV"
                "NORWEGIAN", "NB", "NORWEGIAN_BOKMAL" -> "NB"
                "ROMANIAN", "RO" -> "RO"
                "SLOVAK", "SK" -> "SK"
                "SLOVENIAN", "SL" -> "SL"
                "SWEDISH", "SV" -> "SV"
                "TURKISH", "TR" -> "TR"
                "UKRAINIAN", "UK" -> "UK"
                else -> normalized.replace('_', '-')
            }
        }

private fun OpenAISettings.normalizedDeepLBaseUrl(): String {
    val normalizedBaseUrl = baseUrl.trim().trimEnd('/').removeSuffix("/v2/translate")
    val defaultBaseUrl =
        if (key.endsWith(":fx")) {
            "https://api-free.deepl.com"
        } else {
            "https://api.deepl.com"
        }

    return when {
        normalizedBaseUrl.isBlank() -> defaultBaseUrl
        normalizedBaseUrl.equals("https://api.deepl.com", ignoreCase = true) -> defaultBaseUrl
        normalizedBaseUrl.equals("https://api-free.deepl.com", ignoreCase = true) -> defaultBaseUrl
        else -> normalizedBaseUrl
    }
}

const val LOCAL_TRANSLATION_PROVIDER_URL = "local://translation"

/** Qwen-MT 的领域提示（domains）：引导其按财经新闻语境翻译，保留数字/代码/术语。 */
private const val QWEN_MT_FINANCE_DOMAINS =
    "The text is from US financial and economic news. " +
        "Preserve numbers, percentages, dates, currencies, stock tickers and market indices exactly as-is. " +
        "Use the target language's standard financial terminology."

/**
 * 把 HTML 转成适合 Qwen-MT 翻译的纯文本：块级标签/换行转成段落换行，去掉其余标签。
 */
internal fun htmlToTranslatableText(html: String): String =
    runCatching {
        val document = Jsoup.parse(html)
        document.select("br").append("\n")
        document.select("p, div, li, h1, h2, h3, h4, h5, h6, blockquote, tr").append("\n")
        document
            .wholeText()
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }.getOrDefault(html)

/**
 * 把翻译后的纯文本按空行回填为 <p> 段落，保证在文章页按段落渲染。
 */
internal fun wrapPlainTextInParagraphs(text: String): String =
    text
        .split(Regex("\\n\\s*\\n"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(separator = "\n") { "<p>$it</p>" }
        .ifBlank { "<p>$text</p>" }

/**
 * 把应用内的语言名/语言代码映射为 Qwen-MT 支持的英文语言全称。
 * 应用内目标语言可能是系统语言显示名（如 "中文"/"English"）、ISO 代码（"zh"/"en"）或完整名。
 * 无法识别时返回 null（源语言回退 "auto"，目标语言回退 "Chinese"）。
 */
internal fun toQwenMtLanguageName(language: String): String? {
    val normalized = language.trim().lowercase(Locale.ROOT)
    if (normalized.isBlank() || normalized == "auto") {
        return "auto"
    }
    return when (normalized) {
        "zh", "zh-cn", "zh-hans", "chinese", "simplified chinese", "中文", "简体中文", "汉语" -> "Chinese"
        "zh-tw", "zh-hant", "traditional chinese", "繁体中文" -> "Traditional Chinese"
        "en", "english", "英文", "英语" -> "English"
        "ja", "jp", "japanese", "日文", "日语" -> "Japanese"
        "ko", "kr", "korean", "韩文", "韩语", "朝鲜语" -> "Korean"
        "ru", "russian", "俄文", "俄语" -> "Russian"
        "fr", "french", "法文", "法语" -> "French"
        "de", "german", "德文", "德语" -> "German"
        "es", "spanish", "西班牙文", "西班牙语" -> "Spanish"
        "pt", "portuguese", "葡萄牙文", "葡萄牙语" -> "Portuguese"
        "it", "italian", "意大利文", "意大利语" -> "Italian"
        "nl", "dutch", "荷兰文", "荷兰语" -> "Dutch"
        "pl", "polish", "波兰文", "波兰语" -> "Polish"
        "tr", "turkish", "土耳其文", "土耳其语" -> "Turkish"
        "vi", "vietnamese", "越南文", "越南语" -> "Vietnamese"
        "th", "thai", "泰文", "泰语" -> "Thai"
        "id", "indonesian", "印度尼西亚语" -> "Indonesian"
        "ms", "malay", "马来语" -> "Malay"
        "ar", "arabic", "阿拉伯语" -> "Arabic"
        "hi", "hindi", "印地语" -> "Hindi"
        "uk", "ukrainian", "乌克兰语" -> "Ukrainian"
        "cs", "czech", "捷克语" -> "Czech"
        "el", "greek", "希腊语" -> "Greek"
        "sv", "swedish", "瑞典语" -> "Swedish"
        "hu", "hungarian", "匈牙利语" -> "Hungarian"
        "da", "danish", "丹麦语" -> "Danish"
        "fi", "finnish", "芬兰语" -> "Finnish"
        "bg", "bulgarian", "保加利亚语" -> "Bulgarian"
        "ro", "romanian", "罗马尼亚语" -> "Romanian"
        "he", "hebrew", "希伯来语" -> "Hebrew"
        "yue", "cantonese", "粤语" -> "Cantonese"
        else -> null
    }
}

/** 从 "prefix: HTTP 400 ..." 这类错误信息里提取 HTTP 状态码。 */
private fun extractHttpStatusCode(message: String): Int? =
    Regex("HTTP (\\d{3})")
        .find(message)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()

/** 单块最大字符数（约 1.5–2k token，保证分块译文质量与上下文完整）。 */
private const val MAX_TRANSLATION_CHUNK_LENGTH = 3000

/** 每块最多尝试次数。 */
private const val MAX_TRANSLATION_ATTEMPTS = 3

/** 重试退避基数（毫秒）。 */
private const val RETRY_BACKOFF_MS = 1000L

/** 可重试的 HTTP 状态码（限流与瞬时服务端错误）。 */
private val RETRYABLE_STATUS_CODES = setOf(429, 500, 502, 503, 504)

/** 分块哨兵：正常文本中几乎不可能出现的控制字符。 */
private const val CHUNK_SENTINEL = "\u0001"

/** HTML 块级结束标签（分块边界，标签随块保留）。 */
private val HTML_BLOCK_TAG_REGEX =
    Regex(
        "(</p>|</li>|</h[1-6]>|<br>|</blockquote>|</tr>)",
        RegexOption.IGNORE_CASE,
    )

/**
 * 长文分块：优先在块级边界（HTML 块级标签 / 空行 / 句号）切分，
 * 每块 ≤ [MAX_TRANSLATION_CHUNK_LENGTH] 字符，保证不切断标签与句子。
 */
internal fun chunkTranslationContent(
    content: String,
    preserveHtml: Boolean,
): List<String> {
    if (content.length <= MAX_TRANSLATION_CHUNK_LENGTH) {
        return listOf(content)
    }

    // 注意：Java/Android 正则的 look-behind 必须有界，故 HTML 分支
    // 不用 look-behind（如 (?<=<br\s*/?>) 会抛 PatternSyntaxException），
    // 改为「规整 <br> 变体 → 块级标签后插哨兵 → 按哨兵切分」。
    val pieces: List<String> =
        if (preserveHtml) {
            val normalized =
                content.replace(
                    Regex("<br\\s*/?>", RegexOption.IGNORE_CASE),
                    "<br>",
                )
            val marked =
                HTML_BLOCK_TAG_REGEX.replace(normalized) { match ->
                    match.value + CHUNK_SENTINEL
                }
            marked.split(CHUNK_SENTINEL).filter { it.isNotBlank() }
        } else {
            content
                .split(Regex("(?<=\\n\\n)|(?<=[.!?]\\s)"))
                .filter { it.isNotBlank() }
        }

    val chunks = ArrayList<String>()
    val current = StringBuilder()
    for (piece in pieces) {
        if (current.isNotEmpty() && current.length + piece.length > MAX_TRANSLATION_CHUNK_LENGTH) {
            chunks += current.toString()
            current.clear()
        }
        // 单段超长时按句号硬切（保底）
        if (piece.length > MAX_TRANSLATION_CHUNK_LENGTH) {
            var rest = piece
            while (rest.length > MAX_TRANSLATION_CHUNK_LENGTH) {
                val cut =
                    rest
                        .lastIndexOf(". ", MAX_TRANSLATION_CHUNK_LENGTH)
                        .coerceAtLeast(MAX_TRANSLATION_CHUNK_LENGTH / 2)
                if (current.isNotEmpty()) {
                    chunks += current.toString()
                    current.clear()
                }
                chunks += rest.substring(0, cut + 1).trim()
                rest = rest.substring(cut + 1).trimStart()
            }
            current.append(rest)
        } else {
            current.append(piece)
        }
    }
    if (current.isNotBlank()) {
        chunks += current.toString()
    }
    return chunks.ifEmpty { listOf(content) }
}

/**
 * 明文 http 且非本地/内网地址（本地调试与局域网自建服务放行）。
 * 用于拒绝可能泄露 API Key 的非加密端点。
 */
fun String.isInsecureNonLocalUrl(): Boolean =
    startsWith("http://", ignoreCase = true) &&
        !startsWith("http://localhost", ignoreCase = true) &&
        !startsWith("http://127.", ignoreCase = true) &&
        !startsWith("http://10.", ignoreCase = true) &&
        !startsWith("http://192.168.", ignoreCase = true) &&
        !startsWith("http://172.16.", ignoreCase = true)
