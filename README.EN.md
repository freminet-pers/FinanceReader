# 财经速读 · Finance Reader

[中文](README.md) | **English**

> An Android app for reading U.S. stock-market &amp; economics news with AI translation, built as a fork of [Feeder](https://github.com/spacecowboy/Feeder) (GPL-3.0) for the Chinese market.
> **No Google services required** — plain APK sideload, made for Huawei HarmonyOS (Android 12 compatibility layer) and other GMS-free devices.

---

## What it is

Finance Reader is a fully local RSS reader that ships pre-subscribed to 7 U.S. financial news feeds and adds full-article translation powered by **your own AI API**. Turn English financial news into Chinese (or any language) in one tap — precise, consistent terminology, with numbers and ticker symbols preserved.

## ✨ What we added / improved over Feeder

| Feature | Description |
|---|---|
| 🗞️ Preloaded finance feeds | Auto-subscribes CNBC, MarketWatch, Seeking Alpha, NPR, FRED and more on first launch |
| 🤖 BYOK AI translation | Fill API Key + Base URL + model; supports DeepSeek / Kimi / Zhipu / Qwen and any OpenAI-compatible endpoint; built-in "Test connection" |
| 🎯 Finance-grade prompt | Built-in system prompt: industry-standard terminology, numbers/percentages/currencies/tickers/company names kept as-is, no additions or fabrications |
| 🔤 Offline language detection | Unicode script + system `TextClassifier` + Lingua (no GMS); manual source-language override |
| 🌐 Target follows system language | Target language defaults to the system language, overridable |
| ⚡ One-tap full-text translation | A "Translate" button at the top of the article translates the **full text** in one tap |
| 🔁 Background + cache | Translation keeps running in the background; translated articles reopen already-translated with no extra API cost |
| 📰 Auto-translate list titles | Optional toggle translates every title in the list |
| 🧩 Chunking + retry | Long articles are chunked at block boundaries; 429/5xx retry with backoff; serialized requests to control cost |
| 🔐 Key security | API key encrypted with Android Keystore; HTTPS enforced; OPML export excludes keys |

## 📸 Screenshots

| Feed list | Drawer |
|---|---|
| ![Feed list](screenshots/1-feed-list.png) | ![Drawer](screenshots/2-drawer-feeds.png) |

| Article | Translation settings |
|---|---|
| ![Article](screenshots/3-article.png) | ![Settings](screenshots/4-translation-settings.png) |

| Settings (source language / prompt / test) | Top translate button |
|---|---|
| ![Settings 2](screenshots/5-translation-settings-2.png) | ![Translate button](screenshots/6-translate-button.png) |

## 🔧 How it works

```
RSS fetch → text extraction → offline language detection → system prompt + source → OpenAI-compatible API → translation → local cache → display
```

- **Language detection**: Unicode script prefilter (kana→ja, hangul→ko, Han→zh) → system `TextClassifier` (API 29+, AOSP) → `pemistahl/lingua` fallback → simplified/traditional heuristics.
- **Translation**: user-provided `{baseURL}/chat/completions`, Bearer auth; non-streaming; long articles chunked at HTML block boundaries and joined.
- **Security**: API key stored with Android Keystore AES-256-GCM; non-local plaintext `http://` endpoints rejected; no key in logs.

## 📦 Install

1. Download `app-fdroid-release.apk` (arm64-v8a).
2. On Huawei HarmonyOS: disable "Pure Mode" → allow "unknown sources" → tap install, choose "install anyway" on the risk prompt.
3. The app auto-subscribes finance feeds on first launch; configure your API in Settings → AI and translation → Translation API, tap "Test connection", then translate.

> ⚠️ Do not upgrade the phone to HarmonyOS NEXT (5.x) — it no longer runs Android APKs.

See `USER_GUIDE.md` for a detailed user guide (Chinese).

## 🛠️ Build from source

```bash
# Requires JDK 17+ and Android SDK 36
./gradlew assembleFdroidRelease
```

See `BUILD_GUIDE.md` for build, signing and versioning rules.

## 📄 License

Forked from [Feeder](https://github.com/spacecowboy/Feeder), licensed under **GPL-3.0** (see `LICENSE`).

## 🙏 Credits

Thanks to the Feeder authors and community; language detection by [Lingua](https://github.com/pemistahl/lingua) (MIT); AI calls via [openai-kotlin](https://github.com/aallam/openai-kotlin) (MIT).
