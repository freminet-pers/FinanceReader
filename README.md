# 财经速读 · Finance Reader

**中文** | [English](README.EN.md)

给爸妈（也给自己）用的美股财经新闻阅读器：把几个靠谱的财经源订进来，英文新闻一键翻成中文，翻译得还像那么回事——数字、股票代码、术语都不会翻错。

这个项目是在开源阅读器 [Feeder](https://github.com/spacecowboy/Feeder)（GPL-3.0）基础上改的。Feeder 本来就是个很干净的本地 RSS 阅读器，我们没动它那些已经做好的部分，只在上面补了「财经内容」和「翻译」这两件事。

---

## 支持什么

**订阅源格式**

- RSS、Atom、JSON Feed，随手粘贴一个网址就能加。
- OPML 导入 / 导出（导出就是一份备份）。
- 离线阅读、未读计数、收藏、桌面小组件、全文抓取……Feeder 原有的能力都还在。

**系统要求**

- Android 10（API 29）及以上，arm64 设备。
- 不依赖谷歌服务，不用注册账号，所有数据都在本地。

---

## 我们具体做了什么

1. **开箱就有财经内容**：第一次打开自动订好 7 个美国财经源（CNBC、MarketWatch、Seeking Alpha、NPR、FRED），联网就能刷出新闻，不用自己一个一个加。

2. **自带 AI 翻译，用的是你自己的 key**：设置里填 API Key、接口地址、模型名——DeepSeek、Kimi、智谱、通义，只要是 OpenAI 兼容接口都能用——点一下「测试连接」确认没问题，就能翻译整篇文章。

3. **翻译是奔着财经新闻去的**：内置提示词专门调过——数字、百分比、日期、货币、股票代码、公司名一律原样保留；术语用行业标准译法（「basis points」就是「基点」）；不添油加醋、不凭空编造。

4. **自动认原文语言**：完全离线的语言识别（不靠谷歌），英文、日文、韩文、简繁中文都能认；拿不准就手动指定源语言。目标语言默认跟手机系统走。

5. **一键翻全文**：文章顶部一个「翻译」按钮，点一下直接出全文中文，不用先去「展开全文」再翻译。翻过的文章，下次打开直接就是译文，也不会重复花 API 的钱。

6. **长文不翻车**：特别长的文章自动分块翻译，失败了自动重试；翻译在后台继续，切走页面也不影响。

7. **安全上没含糊**：API Key 用系统级加密存本地，只走 HTTPS，日志和导出文件里都不会带出密钥。

---

## 截图

| 文章列表 | 订阅源 |
|---|---|
| ![文章列表](screenshots/1-feed-list.png) | ![订阅源](screenshots/2-drawer-feeds.png) |

| 文章正文 | 翻译设置 |
|---|---|
| ![文章](screenshots/3-article.png) | ![翻译设置](screenshots/4-translation-settings.png) |

| 设置（源语言 / 提示词 / 测试连接） | 顶部一键翻译按钮 |
|---|---|
| ![设置](screenshots/5-translation-settings-2.png) | ![翻译按钮](screenshots/6-translate-button.png) |

---

## 安装

1. 到 [Releases](../../releases) 下载 `app-fdroid-release.apk`（arm64）。
2. 手机上允许「未知来源」安装，装完打开即自动订阅财经源。
3. 去 设置 → AI and translation → Translation API 填好你的 API，点「测试连接」成功后就能翻译。

更详细的图文说明见 [`USER_GUIDE.md`](USER_GUIDE.md)。

## 从源码构建

```bash
# 需要 JDK 17+ 和 Android SDK 36
./gradlew assembleFdroidRelease
```

构建、签名和版本号规则见 [`BUILD_GUIDE.md`](BUILD_GUIDE.md)。

## 许可

基于 [Feeder](https://github.com/spacecowboy/Feeder) 二次开发，遵循 **GPL-3.0**（见 `LICENSE`）。

## 致谢

感谢 Feeder 的作者和社区；语言识别用 [Lingua](https://github.com/pemistahl/lingua)（MIT），AI 调用用 [openai-kotlin](https://github.com/aallam/openai-kotlin)（MIT）。
