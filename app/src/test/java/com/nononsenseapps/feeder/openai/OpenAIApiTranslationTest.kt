package com.nononsenseapps.feeder.openai

import com.aallam.openai.api.chat.ChatChoice
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.exception.AuthenticationException
import com.aallam.openai.api.exception.OpenAIError
import com.aallam.openai.api.exception.OpenAIErrorDetails
import com.aallam.openai.api.exception.UnknownAPIException
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.nononsenseapps.feeder.archmodel.OpenAISettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** 按脚本依次返回/抛错的假客户端。 */
class ScriptedOpenAIClient(
    private val scripts: List<() -> ChatCompletion>,
) : OpenAIClient {
    var callCount = 0
        private set

    override suspend fun models(requestOptions: RequestOptions?): List<Model> = emptyList()

    override suspend fun chatCompletion(
        request: ChatCompletionRequest,
        requestOptions: RequestOptions?,
    ): ChatCompletion {
        val script = scripts.getOrNull(callCount) ?: error("unexpected extra call")
        callCount++
        return script()
    }
}

class OpenAIApiTranslationTest {
    private val settings =
        OpenAISettings(
            key = "test-key",
            modelId = "test-model",
            baseUrl = "https://api.example.com/v1/",
        )

    @Test
    fun retriesOnServerErrorThenSucceeds() =
        runTest {
            val client =
                ScriptedOpenAIClient(
                    listOf(
                        {
                            throw UnknownAPIException(
                                statusCode = 500,
                                error =
                                    OpenAIError(
                                        detail =
                                            OpenAIErrorDetails(
                                                message = "Server Error",
                                                code = "server_error",
                                            ),
                                    ),
                            )
                        },
                        { response("translated") },
                    ),
                )
            val api = OpenAIApi("en") { client }
            val result = api.translate("hello", "zh", settings)

            assertIs<OpenAIApi.TranslationResult.Success>(result)
            assertEquals(2, client.callCount)
        }

    @Test
    fun failsFastOnAuthErrorWithoutRetry() =
        runTest {
            val client =
                ScriptedOpenAIClient(
                    listOf(
                        {
                            throw AuthenticationException(
                                statusCode = 401,
                                error =
                                    OpenAIError(
                                        detail =
                                            OpenAIErrorDetails(
                                                message = "Unauthorized",
                                                code = "invalid_api_key",
                                            ),
                                    ),
                            )
                        },
                    ),
                )
            val api = OpenAIApi("en") { client }
            val result = api.translate("hello", "zh", settings)

            assertIs<OpenAIApi.TranslationResult.Error>(result)
            assertEquals(1, client.callCount)
        }

    @Test
    fun longTextIsTranslatedInChunksAndJoined() =
        runTest {
            val sentence = "The Federal Reserve raised interest rates by 25 basis points. "
            val longContent = sentence.repeat(200) // ~12k chars → ≥4 chunks
            val expectedChunks = chunkTranslationContent(longContent, preserveHtml = false).size
            val client =
                ScriptedOpenAIClient(
                    List(expectedChunks + 2) { index ->
                        { response("【译文】chunk$index") }
                    },
                )
            val api = OpenAIApi("en") { client }
            val result = api.translate(longContent, "zh", settings)

            val success = assertIs<OpenAIApi.TranslationResult.Success>(result)
            assertEquals(expectedChunks, client.callCount)
            assertEquals(
                (0 until expectedChunks).joinToString("\n\n") { "【译文】chunk$it" },
                success.content,
            )
        }

    @Test
    fun htmlIsChunkedAtBlockBoundaries() =
        runTest {
            val block = "<p>The market rallied strongly today, with the S&P 500 gaining 1.2%.</p>"
            val longHtml = block.repeat(100) // ~6.5k chars → ≥3 chunks
            val expectedChunks = chunkTranslationContent(longHtml, preserveHtml = true).size
            val client =
                ScriptedOpenAIClient(
                    List(expectedChunks + 2) { { response(block) } },
                )
            val api = OpenAIApi("en") { client }
            val result = api.translate(longHtml, "zh", settings, preserveHtml = true)

            assertIs<OpenAIApi.TranslationResult.Success>(result)
            assertEquals(expectedChunks, client.callCount)
        }

    private fun response(content: String) =
        ChatCompletion(
            id = "test",
            model = ModelId(id = "test-model"),
            created = 0,
            choices =
                listOf(
                    ChatChoice(
                        index = 0,
                        message =
                            ChatMessage(
                                role = ChatRole.Assistant,
                                content = content,
                            ),
                        finishReason = null,
                        logprobs = null,
                    ),
                ),
            usage = null,
            systemFingerprint = null,
        )
}
