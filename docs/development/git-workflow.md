# Git 工作流

- 从目标分支建立单一目的的短生命周期分支。
- 修改前检查工作区，保留他人或用户已有变更。
- 每个提交聚焦一个 Starter 或一个清晰的跨模块治理事项。
- 合并前检查 `git diff`、运行测试并同步文档。
- 发布标签采用 `v<major>.<minor>.<patch>`，必须与根 POM 的 `revision` 一致。
- 新增模块、破坏公共 API 或升级平台基线时，在 `docs/decisions/` 增加 ADR。
