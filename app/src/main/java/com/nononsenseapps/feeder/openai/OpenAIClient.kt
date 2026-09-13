package com.nononsenseapps.feeder.openai

import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.core.RequestOptions
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.Model
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.nononsenseapps.feeder.archmodel.OpenAISettings
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.url
import io.ktor.http.appendPathSegments
import kotlin.time.Duration.Companion.seconds

interface OpenAIClient {
    suspend fun models(requestOptions: RequestOptions? = null): List<Model>

    suspend fun chatCompletion(
        request: ChatCompletionRequest,
        requestOptions: RequestOptions?,
    ): ChatCompletion
}

class OpenAIClientDefault(
    settings: OpenAISettings,
) : OpenAIClient {
    private val client = OpenAI(config = settings.toOpenAIConfig())

    override suspend fun models(requestOptions: RequestOptions?): List<Model> = client.models(requestOptions)

    override suspend fun chatCompletion(
        request: ChatCompletionRequest,
        requestOptions: RequestOptions?,
    ): ChatCompletion = client.chatCompletion(request, requestOptions)
}

private fun OpenAISettings.toOpenAIConfig(): OpenAIConfig =
    OpenAIConfig(
        token = key,
        // 全文/长文翻译生成较慢：请求和 socket 超时都要覆盖整次生成；连接单独限短。
        timeout =
            Timeout(
                request = timeoutSeconds.coerceIn(120, 600).seconds,
                connect = OPENAI_CONNECT_TIMEOUT_SECONDS.seconds,
                socket = timeoutSeconds.coerceIn(120, 600).seconds,
            ),
        // LogLevel.None：任何情况下都不输出请求头（避免 Authorization 落入日志）
        logging = LoggingConfig(logLevel = LogLevel.None),
        host = toOpenAIHost(withAzureDeploymentId = false),
        httpClientConfig = {
            if (isAzure) {
                install(HttpSend)
                install("azure-interceptor") {
                    plugin(HttpSend).intercept { request ->
                        request.headers.remove("Authorization")
                        request.headers.append("api-key", key)
                        // models path doesn't include azureDeploymentId
                        val path = request.url.pathSegments.takeLastWhile { it != "openai" || it.isEmpty() }
                        val url =
                            toOpenAIHost(withAzureDeploymentId = path.last() != "models")
                                .toUrl()
                                .appendPathSegments(path)
                                .build()
                        request.url(url)
                        execute(request)
                    }
                }
            }
        },
    )

private const val OPENAI_CONNECT_TIMEOUT_SECONDS = 30
