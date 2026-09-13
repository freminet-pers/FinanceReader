# 财经速读 · Finance Reader

[中文](README.md) | **English**

An Android RSS reader focused on financial news. It ships with a set of U.S. market feeds and can translate article titles, RSS descriptions, and full text into Chinese or another selected target language using your own AI API.

This project is based on the open-source [Feeder](https://github.com/spacecowboy/Feeder) reader (GPL-3.0). It keeps Feeder's mature local reading experience and adds finance-focused feeds and translation workflows.

Current stable version: <code>v2.23.6</code> (versionCode <code>4058</code>)

## What changed in this release

- **DeepSeek V4.1 Flash and Qwen-MT Flash are first-class choices**: select a provider in Settings and the model and endpoint are filled in automatically; only your API key is required.
- **Automatic title translation on app open**: when “Auto-translate all article titles” is enabled, uncached titles in the current feed list are translated sequentially in the background.
- **Cached titles remain visible**: “Show translated titles” is a separate switch, so titles already translated remain available after automatic translation is turned off.
- **The list and article page share the same translated title**: opening an article reuses the cached title instead of requesting it again.
- **RSS descriptions translate on article open**: after you open an article, its title and existing RSS description are translated automatically. Full text remains behind the top “Translate” action so opening a preview does not immediately trigger a large request.
- **Financial-text safeguards**: the translation flow is tuned to preserve numbers, percentages, dates, currencies, tickers, indices, company names, and HTML structure as far as possible.
- **Complete locale coverage**: the new provider, automatic translation, and cached-title visibility strings are present in all existing app locales.

## Recommended providers

| Provider | Default model | Default endpoint | Best for |
| --- | --- | --- | --- |
| **DeepSeek V4.1 Flash** | <code>deepseek-flash</code> | <code>https://api.deepseek.com</code> | Recommended general choice for a strong speed/quality balance on financial news |
| **Qwen-MT Flash** | <code>qwen-mt-flash</code> | <code>https://dashscope.aliyuncs.com/compatible-mode/v1</code> | Translation-only workloads that need consistent speed and quality |

### DeepSeek V4.1 Flash optimizations

- Uses DeepSeek's Chat Completions route and accepts endpoints entered with or without <code>/v1</code>, without duplicating the path.
- Disables unnecessary thinking output for V4/V4.1 translation requests, reducing latency and extra output for titles and previews.
- Uses the preset model and direct request path for the official provider, avoiding an extra model-discovery request.
- Retries rate limits and transient server failures a limited number of times; if one article still fails, its original text remains available.

### Qwen-MT Flash optimizations

- Uses Qwen-MT's dedicated request format instead of treating the model like a general chat model.
- Sends <code>translation_options.source_lang</code> and <code>translation_options.target_lang</code> explicitly and sets the finance domain automatically.
- Sends only the <code>user</code> message accepted by the translation model; the incompatible generic <code>system</code> message is omitted.
- Works for titles, RSS descriptions, and full-text translation. Qwen-MT is translation-only and is not used for AI article summaries.

## Translation workflow

1. Open **Settings → AI and translation → Translation API**.
2. Choose **DeepSeek V4.1 Flash** or **Qwen-MT Flash** and confirm the prefilled model and endpoint.
3. Enter your API key and choose a target language. Source language can stay on offline auto-detection or be selected manually.
4. Optionally edit the custom system prompt. Qwen-MT uses dedicated translation parameters and does not send the generic system prompt.
5. Tap **Test connection**, then save.
6. Enable **Auto-translate all article titles** to process uncached titles in the background when the app opens. Turn it off to stop new requests, and leave **Show translated titles** enabled if you still want to see cached results.
7. Open an article to see its cached translated title and automatically translate its RSS description. Use the top **Translate** action when you decide to read the full text.

Results are cached per article and translation configuration. Changes to the model, endpoint, source language, target language, or prompt create a new cache identity, preventing an old configuration from being shown as a new result. Long content is chunked, requests are serialized, and recoverable errors are retried to balance latency, stability, and API usage.

## Included finance feeds

The first launch subscribes to 12 feeds:

CNBC Top News, CNBC Markets, MarketWatch Top Stories, MarketWatch Market Pulse, Yahoo Finance, WSJ Markets, Nasdaq Markets, NYT Economy, Fortune, Seeking Alpha, NPR Business, and FRED Blog.

You can add any RSS, Atom, or JSON Feed and import or export subscriptions with OPML.

## Main capabilities

- RSS, Atom, and JSON Feed parsing
- Offline reading, background sync, unread counts, bookmarks, and home-screen widgets
- Full-text fetching, article search, and OPML import/export
- AI translation, on-device offline translation, and DeepL
- OpenAI, Azure OpenAI, and other Chat Completions-compatible providers
- Custom source language, target language, and finance-oriented translation prompts

## Privacy and API keys

- No API key is bundled. Translation costs are charged by the provider account you configure.
- The API key is encrypted with Android Keystore and stored locally; it is not written to logs, OPML exports, or article data.
- Remote endpoints are required to use HTTPS by default; HTTP is accepted only for explicitly local or LAN addresses.
- No app account is required. Subscriptions and article data stay on the device by default.

## Install and build

Stable APK: [Finance Reader v2.23.6 Release](https://github.com/freminet-pers/FinanceReader/releases/tag/v2.23.6). You can also build it yourself:

    ./gradlew :app:assembleFdroidRelease

Build requirements are JDK 17+ and Android SDK 36. The minimum supported version is Android 10 (API 29), and the current release target is arm64.

<code>FdroidRelease</code> is the existing Gradle build-variant name, not the product name. The app is branded **财经速读** and uses applicationId <code>com.financereader.app</code>; the variant keeps the no-Google-services release configuration.

Before submitting changes, run:

    ./gradlew :app:ktlintCheck
    ./gradlew :app:testFdroidDebugUnitTest

See [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENTS.md](AGENTS.md) for contribution and repository guidance.

## License

This project is a fork and extension of [Feeder](https://github.com/spacecowboy/Feeder), licensed under **GPL-3.0**; see [LICENSE](LICENSE).

Thanks to the Feeder authors and community. Language detection uses [Lingua](https://github.com/pemistahl/lingua), and AI calls use [openai-kotlin](https://github.com/aallam/openai-kotlin).
