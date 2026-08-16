# 财经速读 · Finance Reader

**中文** | [English](README.EN.md)

> 一个面向中国市场的「美股财经 RSS 阅读 + AI 翻译」安卓应用，基于开源项目 [Feeder](https://github.com/spacecowboy/Feeder)（GPL-3.0）二次开发。
> **无需谷歌服务**，标准 APK 直装，适配华为鸿蒙（Android 12 兼容层）等无 GMS 设备。

---

## 这是做什么的

财经速读是一个完全本地运行的 RSS 阅读器，开箱即订阅了 7 个美国财经新闻源，并内置了基于你**自备 AI API** 的整篇文章翻译能力——把英文财经新闻一键译成中文（或任意语言），翻译精准、术语统一、数字与股票代码原样保留。

## ✨ 相比上游 Feeder，我们新增 / 改进了什么

| 功能 | 说明 |
|---|---|
| 🗞️ 预置美股财经源 | 首启自动订阅 CNBC、MarketWatch、Seeking Alpha、NPR、FRED 等 7 个源 |
| 🤖 自配 AI 翻译 | 填 API Key + Base URL + 模型名即可；支持 DeepSeek / Kimi / 智谱 / 通义等所有 OpenAI 兼容端点；内置「测试连接」 |
| 🎯 财经精译提示词 | 内置系统提示词：术语用行业标准译法、数字/百分比/货币/股票代码/公司名原样保留、不增删不发挥 |
| 🔤 离线语言识别 | Unicode 脚本 + 系统 TextClassifier + Lingua 三级识别，**无 GMS**；可手动指定源语言 |
| 🌐 目标语言跟随系统 | 目标语言默认跟随系统语言，也可手动选择 |
| ⚡ 一键翻全文 | 正文顶部「翻译」按钮，点一下直接翻译**全文**（无需先展开再翻译） |
| 🔁 后台翻译 + 缓存 | 翻译在后台继续；翻过的文章下次打开直接显示译文，不重复计费 |
| 📰 列表标题自动翻译 | 设置里开启后，列表里所有标题自动翻译 |
| 🧩 长文分块 + 重试 | 长文按块级边界分块翻译，429/5xx 自动退避重试，全局串行控费 |
| 🔐 密钥安全 | API Key 用 Android Keystore 加密存储；强制 HTTPS；OPML 导出不含密钥 |

## 📸 截图

| 文章列表 | 订阅源抽屉 |
|---|---|
| ![文章列表](screenshots/1-feed-list.png) | ![订阅源](screenshots/2-drawer-feeds.png) |

| 文章正文 | 翻译设置 |
|---|---|
| ![文章](screenshots/3-article.png) | ![翻译设置](screenshots/4-translation-settings.png) |

| 设置（源语言 / 提示词 / 测试连接） | 顶部一键翻译按钮 |
|---|---|
| ![设置2](screenshots/5-translation-settings-2.png) | ![翻译按钮](screenshots/6-translate-button.png) |

## 🔧 工作原理

```
RSS 拉取 → 正文提取 → 语言识别(离线) → 系统提示词 + 原文 → OpenAI 兼容 API → 译文 → 本地缓存 → 展示
```

- **语言识别**：Unicode 文字脚本粗筛（假名→日文、谚文→韩文、纯汉字→中文）→ 系统 `TextClassifier`（API 29+，AOSP 原生）→ `pemistahl/lingua` 兜底 → 简繁字表。
- **翻译**：用户自填 `{baseURL}/chat/completions`，Bearer 认证；v1 非流式；长文按 HTML 块级标签分块串行翻译后合并。
- **安全**：API Key 以 Android Keystore AES-256-GCM 加密落盘；非本地明文 http 端点直接拒绝；日志不回显密钥。

## 📦 安装

1. 下载 `app-fdroid-release.apk`（arm64-v8a）。
2. 打开应用即自动订阅财经源；在 设置 → AI and translation → Translation API 里填入你的 API 配置，点「Test connection」成功后即可翻译。

> ⚠️ 请勿将手机升级到鸿蒙 NEXT（5.x）——它不再支持安卓 APK。

详细安装与使用说明见仓库根目录的 `USER_GUIDE.md`。

## 🛠️ 从源码构建

```bash
# 需要 JDK 17+ 与 Android SDK 36
./gradlew assembleFdroidRelease
```

构建、签名、版本号规则详见 `BUILD_GUIDE.md`。

## 📄 许可

本项目基于 [Feeder](https://github.com/spacecowboy/Feeder) 二次开发，遵循 **GPL-3.0**（见 `LICENSE`）。

## 🙏 致谢

感谢 Feeder 原作者与社区；语言识别使用 [Lingua](https://github.com/pemistahl/lingua)（MIT）；AI 调用使用 [openai-kotlin](https://github.com/aallam/openai-kotlin)（MIT）。
