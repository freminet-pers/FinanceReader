package com.nononsenseapps.feeder.openai

import com.nononsenseapps.feeder.archmodel.OpenAISettings
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DeepSeekV41Test {
    @Test
    fun usesV41NonThinkingRequestAndPreservesConfiguredPath() =
        runTest {
            MockWebServer().use { server ->
                server.enqueue(
                    MockResponse()
                        .addHeader("Content-Type", "application/json")
                        .setBody(
                            """
                            {"choices":[{"message":{"role":"assistant","content":"市场上涨。"},"finish_reason":"stop"}]}
                            """.trimIndent(),
                        ),
                )

                val result =
                    createApi().translate(
                        content = "The market rose.",
                        targetLanguage = "zh-Hans",
                        settings =
                            OpenAISettings(
                                key = "deepseek-key",
                                modelId = "deepseek-flash",
                                baseUrl = server.url("/v1/").toString(),
                            ),
                        preserveHtml = true,
                        systemPrompt = "Translate from {source_language} to {target_language}.",
                        sourceLangHint = "English",
                    )

                val success = assertIs<OpenAIApi.TranslationResult.Success>(result)
                assertEquals("市场上涨。", success.content)

                val request = server.takeRequest()
                assertEquals("/v1/chat/completions", request.path)
                assertEquals("Bearer deepseek-key", request.getHeader("Authorization"))
                val body = request.body.readUtf8()
                assertTrue(body.contains("\"model\":\"deepseek-flash\""))
                assertTrue(body.contains("\"thinking\":{\"type\":\"disabled\"}"))
                assertTrue(body.contains("\"stream\":false"))
                assertTrue(body.contains("Preserve all HTML tags"))
            }
        }

    @Test
    fun retriesTransientDeepSeekFailure() =
        runTest {
            MockWebServer().use { server ->
                server.enqueue(
                    MockResponse()
                        .setResponseCode(503)
                        .setBody("""{"error":{"message":"temporarily unavailable"}}"""),
                )
                server.enqueue(
                    MockResponse()
                        .addHeader("Content-Type", "application/json")
                        .setBody(
                            """{"choices":[{"message":{"content":"ok"},"finish_reason":"stop"}]}""",
                        ),
                )

                val result =
                    createApi().translate(
                        content = "hello",
                        targetLanguage = "zh",
                        settings =
                            OpenAISettings(
                                key = "test-key",
                                modelId = "deepseek-v4-flash",
                                baseUrl = server.url("/").toString(),
                            ),
                    )

                assertIs<OpenAIApi.TranslationResult.Success>(result)
                assertEquals(2, server.requestCount)
            }
        }

    @Test
    fun exposesProviderErrorWithoutRetryingClientError() =
        runTest {
            MockWebServer().use { server ->
                server.enqueue(
                    MockResponse()
                        .setResponseCode(400)
                        .setBody("""{"error":{"message":"invalid model"}}"""),
                )

                val result =
                    createApi().translate(
                        content = "hello",
                        targetLanguage = "zh",
                        settings =
                            OpenAISettings(
                                key = "test-key",
                                modelId = "deepseek-v4-pro",
                                baseUrl = server.url("/").toString(),
                            ),
                    )

                val error = assertIs<OpenAIApi.TranslationResult.Error>(result)
                assertTrue(error.content.contains("HTTP 400"))
                assertTrue(error.content.contains("invalid model"))
                assertEquals(1, server.requestCount)
            }
        }

    @Test
    fun normalizesOfficialEndpointAndSkipsModelsDiscovery() =
        runTest {
            val settings =
                OpenAISettings(
                    key = "test-key",
                    modelId = "deepseek-flash",
                    baseUrl = "https://api.deepseek.com/v1/",
                )

            assertEquals(
                "https://api.deepseek.com/chat/completions",
                settings.toDeepSeekChatCompletionsUrl(),
            )
            val models = createApi().listModelIds(settings)
            val success = assertIs<OpenAIApi.ModelsResult.Success>(models)
            assertEquals(listOf("deepseek-flash"), success.ids)
        }

    private fun createApi() = OpenAIApi("en") { error("DeepSeek uses the raw JSON path") }
}
