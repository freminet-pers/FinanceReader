# Contributing to FinanceReader

Thanks for helping improve FinanceReader. This repository is a fork and extension of [Feeder](https://github.com/spacecowboy/Feeder), so focused changes, clear attribution, and reproducible checks are especially useful.

## Before you start

- Read the [project scope note](docs/PROJECT_SCOPE.md) to understand what is maintained here and what remains inherited from Feeder.
- Search existing [issues](https://github.com/freminet-pers/FinanceReader/issues) and pull requests before opening a new one.
- Never commit API keys, `local.properties`, signed release material, private feed exports, or article content that you do not have permission to redistribute.
- Keep unrelated upstream churn out of a FinanceReader pull request. Documentation-only changes are welcome when they improve a specific user or maintainer path.

## Local setup

Clone this repository and open it in Android Studio, or use the Gradle wrapper from a terminal:

```bash
git clone https://github.com/freminet-pers/FinanceReader.git
cd FinanceReader
./gradlew :app:assembleFdroidRelease
```

The project currently expects JDK 17+ and Android SDK 36. Building does not require an AI provider account or API key. The release variant is `FdroidRelease`; the app applicationId is `com.financereader.app`.

## Checks

For Kotlin or resource changes, run the smallest relevant checks locally and report the exact commands and outcome in your pull request:

```bash
./gradlew :app:ktlintCheck
./gradlew :app:testFdroidDebugUnitTest
./gradlew :app:assembleFdroidRelease
```

If Android SDK or emulator availability prevents a check, say so explicitly rather than implying that it passed. For documentation-only changes, verify Markdown links and YAML syntax and explain which application checks were not needed.

## Change boundaries

- Keep pull requests focused on one concern and avoid changing business behavior in a documentation or repository-governance PR.
- Preserve GPL-3.0 notices, Feeder attribution, contributor links, and dependency licenses.
- If a change alters Room schema, include the migration and migration test required by the inherited Feeder architecture; see [AGENTS.md](AGENTS.md) for the detailed rule.
- Translation provider changes must keep API keys user-supplied and opt-in. Do not add a bundled key or silently send article text to a new endpoint.
- Default feed changes should update [docs/FEEDS.md](docs/FEEDS.md) and explain why the source was added, removed, or replaced.
- User-facing behavior changes should update both [README.md](README.md) and [README.EN.md](README.EN.md) when practical.

## Pull requests

Use the repository pull-request template. Include:

- what changed and why;
- files or user paths affected;
- commands run and their results;
- screenshots or recordings only when they are real and relevant (do not add placeholders);
- any known risk, provider cost implication, follow-up documentation, or maintainer decision still needed.

Use a short Conventional Commit-style subject when possible, for example:

```text
docs: clarified translation setup and feed replacement
ci: checked documentation links on pull requests
fix: preserved cached title translations after restart
```

## Reporting problems and suggesting changes

Use the issue forms with the app version, Android version, device, source of the APK, and reproducible steps. Remove API keys and private article text from logs. A feed-specific report should include the public feed URL only when it is safe to share.

Security-sensitive reports belong in [SECURITY.md](SECURITY.md), not a public issue.

