# 财经速读 · FinanceReader

[![最新稳定版](https://img.shields.io/github/v/release/freminet-pers/FinanceReader?display_name=tag&sort=semver)](https://github.com/freminet-pers/FinanceReader/releases/latest)
[![CI](https://github.com/freminet-pers/FinanceReader/actions/workflows/ci_build.yml/badge.svg?branch=main)](https://github.com/freminet-pers/FinanceReader/actions/workflows/ci_build.yml)
[![许可证：GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)

[English](README.EN.md) | **中文**

> 面向美股与宏观财经阅读的 Android RSS 应用：基于 Feeder、本地优先；打开即读预置财经源，需要时用你自己的 AI API（BYOK）翻译标题、摘要或全文。

[下载 v2.23.6 Release](https://github.com/freminet-pers/FinanceReader/releases/tag/v2.23.6) · [财经源说明](docs/FEEDS.md) · [贡献指南](CONTRIBUTING.md) · [安全说明](SECURITY.md) · [查看更新记录](CHANGELOG.md) · [English](README.EN.md)

FinanceReader 是 [Feeder](https://github.com/spacecowboy/Feeder) 的独立 fork/扩展，遵循 GPL-3.0。它保留 Feeder 的本地 RSS 阅读基础，并把默认内容、财经翻译工作流和发布文档集中到本项目维护。

## 项目状态

- 当前公开稳定版本：`v2.23.6`（versionCode `4058`）；Release 页面提供可直接安装的 arm64 APK。
- Android 最低版本：Android 10（API 29）。构建变体和包名见[安装与构建](#安装与构建)。
- 这是一个个人/小规模维护的开源 fork；不承诺固定发布周期，也不是 Feeder 官方发行版。
- 仓库目前没有可核验的设备截图，因此首页用真实操作流程说明使用结果；欢迎提交真实设备截图，但不会用占位图冒充产品效果。

## 适合谁 / 不适合谁

**适合：** 想在手机上集中阅读财经 RSS、保留本地阅读数据，并在需要时用自己的翻译服务降低语言门槛的人。

**不适合：** 需要投资建议或行情交易终端的人；不愿自行配置 API Key 或承担服务商费用的人；要求所有财经源永久可用、全文始终免费，或希望获得 Feeder 官方支持的人。

## 与 Feeder 的关系

- **继承：** RSS、Atom、JSON Feed 解析，离线阅读、同步、收藏、搜索、OPML 导入/导出等通用阅读基础来自 Feeder 代码库。
- **独立贡献：** 预置 12 个财经源、财经内容翻译提示与保护规则、标题/摘要/全文翻译工作流、翻译缓存，以及与本项目发布相关的构建和文档。
- **边界：** FinanceReader 不是 Feeder 官方版本；上游的 issue、版本和支持渠道不会自动代表本项目。FinanceReader 的功能问题请在本仓库提交。
- **许可：** 本项目继续遵循 GPL-3.0，并保留 Feeder 与其他依赖的归属信息；详见 [LICENSE](LICENSE) 与[项目范围说明](docs/PROJECT_SCOPE.md)。

## 第一次使用：从订阅到翻译

1. 安装 [v2.23.6 Release](https://github.com/freminet-pers/FinanceReader/releases/tag/v2.23.6)，首次启动会添加一组财经 RSS 订阅。
2. 先阅读原文标题和 RSS 摘要；也可以在设置中删除、添加订阅，或用 OPML 导入自己的订阅。
3. 进入 **设置 → AI 和翻译 → 翻译 API**，选择已有预设或填写兼容 Chat Completions 的服务商、接口地址和模型。
4. 填入**自己的** API Key，选择目标语言，点击 **测试连接** 后保存。API Key 不由本项目提供。
5. 可选开启“自动翻译所有文章标题”：未缓存标题会在后台依次处理；关闭自动翻译后，已缓存的译文仍可按“显示已翻译标题”开关显示。
6. 打开文章时会复用已缓存标题并翻译 RSS 摘要；需要阅读全文时再点击顶部的“翻译全文”，避免打开预览就产生大请求。

翻译结果按文章和翻译配置缓存。模型、接口、源语言、目标语言或提示词变化后，会产生新的缓存标识；长文会分块处理，请求按序发送，临时错误可能重试。缓存可以减少重复请求，但不保证零费用或固定翻译质量。

## 预置财经源

默认源覆盖综合新闻、市场动态、企业/投资媒体与宏观数据，选择依据和可替换方式见[财经源说明](docs/FEEDS.md)。源来自第三方出版方，可能改 URL、限流、删改内容或暂时不可用；它们不构成推荐或投资意见。

你可以随时添加 RSS、Atom 或 JSON Feed，也可以用 OPML 导入/导出订阅。

## 翻译、成本与隐私边界

- 应用不内置 API Key。DeepSeek、Qwen-MT 等预设只是便捷配置示例，也可以使用其他兼容服务、DeepL 或设备本地离线翻译；供应商选择不会改变本项目的 GPL 许可。
- 翻译费用、限额、数据保留和服务可用性由你配置的服务商决定。自动翻译标题可能产生多次请求；请先用少量文章测试并查看服务商账单/隐私条款。
- API Key 通过 Android Keystore 加密后保存在本地，不写入日志、OPML 导出或文章数据。不要把 Key 粘贴到 issue、日志、截图或 Pull Request 中。
- 启用远程翻译后，发送给服务商的是你选择的文章文本及请求参数；含有敏感信息的文章应先确认服务商政策。远程接口默认要求 HTTPS，本机或局域网地址才允许使用 HTTP。
- 订阅、文章和阅读状态默认保存在设备本地，不要求应用账号。RSS 内容仍由相应出版方提供，应用不保证其准确性、完整性或持续可用性。

## 安装与构建

### 直接安装

从 [FinanceReader v2.23.6 Release](https://github.com/freminet-pers/FinanceReader/releases/tag/v2.23.6) 下载 APK。当前 Release 是 arm64 目标，最低支持 Android 10（API 29）；从其他来源安装的 APK 可能使用不同签名，升级前请确认来源一致。

### 从源码构建

需要 JDK 17+ 与 Android SDK 36；构建不需要 API Key。当前发布变体是 `FdroidRelease`，不是产品名称；应用显示名为“财经速读”，applicationId 为 `com.financereader.app`。

```bash
./gradlew :app:assembleFdroidRelease
```

提交前可运行轻量检查：

```bash
./gradlew :app:ktlintCheck
./gradlew :app:testFdroidDebugUnitTest
```

## 文档与协作

- [文档索引](docs/README.md)：按语言进入财经源、项目范围和发布维护说明。
- [贡献指南](CONTRIBUTING.md)：本项目的开发、测试、文档和翻译协作方式。
- [安全报告](SECURITY.md)：API Key 处理和安全问题报告边界。
- [财经源说明](docs/FEEDS.md)：默认 12 个源的覆盖面、URL 和替换方法。
- [项目范围说明](docs/PROJECT_SCOPE.md)：FinanceReader 与 Feeder 的维护边界、归属和 changelog 约定。
- [CHANGELOG](CHANGELOG.md)：FinanceReader 自己的版本记录与保留的 Feeder 上游历史；文件顶部有来源说明。

## 许可与第三方归属

FinanceReader 基于 [Feeder](https://github.com/spacecowboy/Feeder) 二次开发，遵循 [GNU GPL-3.0](LICENSE)。语言识别使用 [Lingua](https://github.com/pemistahl/lingua)，AI 调用使用 [openai-kotlin](https://github.com/aallam/openai-kotlin)；其他依赖的许可证以各自项目和源码中的声明为准。

财经内容、RSS/Atom/JSON Feed 地址及文章版权归相应出版方所有。FinanceReader 只提供本地聚合和可选翻译工具，不代表或再发布这些出版方。
