# 财经速读 · FinanceReader

[中文](README.md) | **English**

> An Android RSS app for U.S. equity and macro news: read a curated set of finance feeds on first launch, then use your own AI API to translate titles, descriptions, or full text when needed.

[Download v2.23.6 Release](https://github.com/freminet-pers/FinanceReader/releases/tag/v2.23.6) · [View the changelog](CHANGELOG.md) · [阅读中文文档](README.md)

FinanceReader is an independent fork and extension of [Feeder](https://github.com/spacecowboy/Feeder), licensed under GPL-3.0. It keeps Feeder's local RSS reading foundation while maintaining finance-focused defaults, translation workflows, and release documentation in this repository.

## Project status

- Current public stable release: `v2.23.6` (versionCode `4058`); the Release page provides an installable arm64 APK.
- Minimum Android version: Android 10 (API 29). See [Install and build](#install-and-build) for the build variant and package name.
- This is a personal/small-maintainer open-source fork. There is no promised release cadence, and it is not an official Feeder distribution.
- The repository currently has no verifiable device screenshots, so the front page documents the real workflow in text. Real device captures are welcome; placeholder images will not be presented as product evidence.

## Who it is for / who it is not for

**Good fit:** people who want to collect finance RSS feeds on Android, keep reading data locally, and use their own translation service when language is a barrier.

**Not a good fit:** people looking for investment advice or a trading terminal; people unwilling to configure an API key or pay a provider; people who need every feed to remain permanently available, full text to always be free, or official Feeder support.

## Compared with Feeder

| Scope | FinanceReader's position |
| --- | --- |
| Inherited foundation | RSS, Atom, and JSON Feed parsing plus the general offline reader, sync, bookmarks, search, and OPML workflows come from the Feeder codebase. |
| Independent contribution | Twelve finance defaults, finance-oriented translation safeguards, title/description/full-text translation workflow, translation caching, and fork-specific build/release documentation. |
| Boundary | FinanceReader is not an official Feeder build. Upstream issues, versions, and support channels do not automatically represent this project; file FinanceReader issues here. |
| License | The project remains GPL-3.0 and keeps Feeder and dependency attribution. See [LICENSE](LICENSE) and the [project scope note](docs/PROJECT_SCOPE.md). |

## First-use workflow: from feeds to translation

1. Install the [v2.23.6 Release](https://github.com/freminet-pers/FinanceReader/releases/tag/v2.23.6); first launch adds a set of finance RSS feeds.
2. Start by reading original titles and RSS descriptions. You can remove or add feeds in Settings, or import your own OPML file.
3. Open **Settings → AI and translation → Translation API**. Choose a preset or enter a Chat Completions-compatible provider, endpoint, and model.
4. Enter **your own** API key, choose a target language, tap **Test connection**, and save. This project does not provide API keys.
5. Optionally enable **Auto-translate all article titles**. Uncached titles are processed sequentially in the background; after turning it off, cached translations can remain visible through **Show translated titles**.
6. Opening an article reuses a cached title and translates the RSS description. Use the top **Translate full text** action when you decide to read the article, so a preview does not automatically create a large request.

Results are cached per article and translation configuration. Changing the model, endpoint, source language, target language, or prompt creates a new cache identity; long text is chunked, requests are serialized, and transient failures may be retried. Caching can reduce duplicate requests, but it does not guarantee zero cost or a fixed translation quality.

## Included finance feeds

The defaults cover general news, market movement, company/investment media, and macroeconomic data. See [Finance feeds](docs/FEEDS.md) for the selection rationale and replacement instructions. Feeds are provided by third-party publishers and may change URLs, rate-limit clients, remove content, or become unavailable; they are not recommendations or investment advice.

You can add any RSS, Atom, or JSON Feed and import or export subscriptions with OPML.

## Translation, cost, and privacy boundaries

- No API key is bundled. DeepSeek and Qwen-MT presets are convenience configurations, not a provider endorsement; other compatible services, DeepL, and on-device offline translation can also be used. Provider choice does not change this project's GPL license.
- Translation price, quotas, retention, and availability are controlled by the provider account you configure. Automatic title translation can generate multiple requests; test with a small set of articles and review the provider's billing and privacy terms.
- API keys are encrypted with Android Keystore and stored locally; they are not written to logs, OPML exports, or article data. Never paste a key into an issue, log, screenshot, or pull request.
- When remote translation is enabled, the selected article text and request parameters are sent to the configured provider. Check that provider's policy before sending sensitive content. Remote endpoints require HTTPS by default; HTTP is accepted only for explicitly local or LAN addresses.
- Subscriptions, articles, and reading state stay on the device by default, and no app account is required. RSS content remains the responsibility of its publisher; the app does not guarantee its accuracy, completeness, or availability.

## Install and build

### Install the release APK

Download the APK from [FinanceReader v2.23.6 Release](https://github.com/freminet-pers/FinanceReader/releases/tag/v2.23.6). The current Release targets arm64 and Android 10 (API 29) or newer. APKs from different sources may use different signing keys, so verify the source before upgrading.

### Build from source

Build requirements are JDK 17+ and Android SDK 36; no API key is needed to build. `FdroidRelease` is the existing release variant name, not the product name. The app is branded “财经速读” and uses applicationId `com.financereader.app`.

```bash
./gradlew :app:assembleFdroidRelease
```

Before submitting changes, run the lightweight checks:

```bash
./gradlew :app:ktlintCheck
./gradlew :app:testFdroidDebugUnitTest
```

## Documentation and collaboration

- [Contributing](CONTRIBUTING.md): project-specific development, testing, documentation, and translation guidance.
- [Security](SECURITY.md): API-key handling and vulnerability-reporting boundaries.
- [Finance feeds](docs/FEEDS.md): coverage, URLs, and replacement guidance for the twelve defaults.
- [Project scope](docs/PROJECT_SCOPE.md): the maintenance boundary, attribution, and changelog convention for this fork.
- [CHANGELOG](CHANGELOG.md): FinanceReader release notes plus retained Feeder upstream history; the file begins with a provenance note.

## License and third-party attribution

FinanceReader is based on [Feeder](https://github.com/spacecowboy/Feeder) and released under the [GNU GPL-3.0](LICENSE). Language detection uses [Lingua](https://github.com/pemistahl/lingua), and AI calls use [openai-kotlin](https://github.com/aallam/openai-kotlin); other dependency licenses are defined by their respective projects and source declarations.

Finance articles, feed URLs, and article copyrights belong to their respective publishers. FinanceReader provides a local aggregation and optional translation tool; it does not represent or republish those publishers.

