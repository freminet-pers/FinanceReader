# 财经速读 · Finance Reader

[English](README.EN.md) | **中文**

一个面向财经新闻的 Android RSS 阅读器。首次打开即可看到预置的美股财经资讯，也可以使用自己的 AI API 将标题、RSS 简介和全文翻译成中文或其他目标语言。

本项目基于开源阅读器 [Feeder](https://github.com/spacecowboy/Feeder)（GPL-3.0）开发，保留了它成熟的 RSS 阅读能力，并针对财经内容和翻译体验做了专门优化。

当前稳定版本：<code>v2.23.6</code>（versionCode <code>4058</code>）

## 这一版重点优化

- **DeepSeek V4.1 Flash 和 Qwen-MT Flash 直接可选**：在设置中选择服务商，模型和接口地址会自动填入，只需填写自己的 API Key。
- **打开应用后自动翻译文章标题**：开启“自动翻译所有文章标题”后，当前列表中尚未缓存的标题会在后台依次翻译，不阻塞阅读。
- **已翻译标题不会消失**：单独的“显示已翻译标题”开关控制缓存结果是否继续显示。关闭自动翻译后，之前已经翻译好的标题仍可保留在列表中。
- **列表与详情页使用同一份标题译文**：点进文章后，详情页标题直接复用列表中的翻译结果，不再重复请求。
- **打开文章自动翻译 RSS 简介**：用户点开感兴趣的文章后，标题和 RSS 中已有的简介会自动翻译；全文仍由顶部的“翻译全文”按钮控制，避免一打开文章就产生过大的请求。
- **财经内容专用保护规则**：尽量保留数字、百分比、日期、货币、股票代码、指数和公司名称，并保留正文 HTML 结构。
- **多语言资源同步**：本次新增的服务商、自动翻译和缓存显示选项已同步到项目现有的全部语言资源。

## 推荐翻译服务

| 服务商 | 默认模型 | 默认接口 | 适合场景 |
| --- | --- | --- | --- |
| **DeepSeek V4.1 Flash** | <code>deepseek-flash</code> | <code>https://api.deepseek.com</code> | 推荐作为通用默认选择，兼顾速度和财经新闻翻译质量 |
| **Qwen-MT Flash** | <code>qwen-mt-flash</code> | <code>https://dashscope.aliyuncs.com/compatible-mode/v1</code> | 翻译专用模型，适合希望获得稳定速度与质量平衡的场景 |

### DeepSeek V4.1 Flash 做了什么优化

- 使用 DeepSeek 官方 Chat Completions 通道，并兼容用户填写的 <code>/v1</code> 地址，避免重复拼接路径。
- 对 V4/V4.1 模型关闭不必要的 thinking 输出，减少标题和简介翻译的等待时间与额外输出。
- 对官方服务商使用预设模型和直接请求路径，不依赖额外的模型列表发现请求。
- 对限流和临时服务端错误进行有限次数重试，失败时保留原文，不让单篇文章影响整个列表。

### Qwen-MT Flash 做了什么优化

- 使用 Qwen-MT 的专用请求格式，而不是把它当作普通聊天模型调用。
- 通过 <code>translation_options.source_lang</code> 和 <code>translation_options.target_lang</code> 明确传递源语言、目标语言，并自动设置财经领域。
- 只发送模型要求的 <code>user</code> 消息，不发送不兼容的 <code>system</code> 消息；因此 Qwen-MT 会使用内置的专用翻译设置，而不是通用系统提示词。
- 适用于标题、RSS 简介和全文翻译；Qwen-MT 本身是翻译服务，不用于 AI 文章摘要功能。

## 翻译工作方式

1. 进入 **设置 → AI 和翻译 → 翻译 API**。
2. 选择 **DeepSeek V4.1 Flash** 或 **Qwen-MT Flash**，确认自动填入的模型和接口地址。
3. 填入自己的 API Key，选择目标语言；源语言可以保持“自动检测”，也可以手动指定。
4. 可选填写自定义系统提示词。Qwen-MT 使用专用翻译参数，不会发送通用系统提示词。
5. 点击“测试连接”，成功后保存。
6. 开启“自动翻译所有文章标题”即可在打开应用后后台处理未缓存标题；需要停止继续请求时关闭此开关，并按需保留“显示已翻译标题”。
7. 打开文章时，详情页会显示已缓存的译文标题，并自动翻译 RSS 简介。阅读全文时再点击顶部的“翻译全文”。

翻译结果按文章和翻译配置缓存。模型、接口、源语言、目标语言或提示词变化后会自动使用新的缓存标识，避免把旧配置的结果误当成新结果。长内容会分块处理，网络请求按序进行，并对可恢复错误进行重试，以平衡速度、稳定性和 API 用量。

## 预置财经资讯

首次启动会预置 12 个财经源：

CNBC Top News、CNBC Markets、MarketWatch Top Stories、MarketWatch Market Pulse、Yahoo Finance、WSJ Markets、Nasdaq Markets、NYT Economy、Fortune、Seeking Alpha、NPR Business、FRED Blog。

也可以随时添加自己的 RSS、Atom 或 JSON Feed，并通过 OPML 导入或导出订阅。

## 主要能力

- RSS、Atom、JSON Feed 解析
- 离线阅读、后台同步、未读计数、收藏和桌面小组件
- 全文抓取、文章搜索、OPML 导入/导出
- AI 翻译、设备本地离线翻译和 DeepL
- OpenAI、Azure OpenAI 及其他兼容 Chat Completions 的服务商
- 自定义源语言、目标语言和财经翻译提示词

## 隐私与 API Key

- 应用不内置任何 API Key，服务费用由用户自己的服务商账户承担。
- API Key 通过 Android Keystore 加密后保存在本地，不写入日志、OPML 导出或文章数据。
- 远程接口默认要求使用 HTTPS；只有本机或局域网等明确的本地地址允许使用 HTTP。
- 不需要注册应用账号，订阅和文章数据默认保存在设备本地。

## 安装与构建

GitHub 发布 APK 时，请从 [Releases](https://github.com/freminet-pers/FinanceReader/releases) 下载对应版本；也可以自行构建：

    ./gradlew :app:assembleFdroidRelease

构建需要 JDK 17+ 和 Android SDK 36，最低支持 Android 10（API 29），当前发布目标为 arm64。

<code>FdroidRelease</code> 是项目现有的 Gradle 构建变体名称，不是应用品牌名称。应用显示名为 **财经速读**，applicationId 为 <code>com.financereader.app</code>；该变体保留无 Google 服务的发布配置。

提交前建议运行：

    ./gradlew :app:ktlintCheck
    ./gradlew :app:testFdroidDebugUnitTest

贡献和代码规范请参阅 [CONTRIBUTING.md](CONTRIBUTING.md) 与 [AGENTS.md](AGENTS.md)。

## 许可

本项目基于 [Feeder](https://github.com/spacecowboy/Feeder) 二次开发，遵循 **GPL-3.0**，详见 [LICENSE](LICENSE)。

感谢 Feeder 作者与社区；语言识别使用 [Lingua](https://github.com/pemistahl/lingua)，AI 调用使用 [openai-kotlin](https://github.com/aallam/openai-kotlin)。
