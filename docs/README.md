# FinanceReader 文档

这里是 FinanceReader 的项目文档入口。面向用户的快速介绍和安装入口仍在根目录的 [README](../README.md)；本目录只保留需要长期维护的项目说明。

## 从这里开始

- [默认财经源](FEEDS.md)：12 个预置源的覆盖面、当前地址和替换方法。
- [项目范围](PROJECT_SCOPE.md)：FinanceReader 与 Feeder 的继承关系、独立贡献、支持边界和 changelog 来源。
- [v2.23.6 Release 维护说明](releases/v2.23.6.md)：当前公开 Release 的核验结果、可直接采用的正文草案和维护者清单。

English readers: [finance feeds](FEEDS.EN.md), [project scope](PROJECT_SCOPE.md#financereader-project-scope), and the [release maintenance note](releases/v2.23.6.md).

## 仓库结构说明

- 根目录 README、`CONTRIBUTING.md`、`SECURITY.md` 和 `LICENSE` 是公开入口、协作、安全与许可文件。
- `app/`、`gradle/`、`ci/` 和 `scripts/` 保留构建与应用源码；本轮仓库治理没有改动业务实现。
- `CHANGELOG.md` 保留 FinanceReader 版本记录和 Feeder 上游历史；`PROJECT_SCOPE.md` 说明如何区分两部分来源。
- `fastlane/`、`graphics/`、`.build.yml`、`.gitlab-ci.bak` 等上游或发布过程材料仍保留在原位置，作为历史和构建上下文，不作为用户入口，也没有在本轮随意删除。
