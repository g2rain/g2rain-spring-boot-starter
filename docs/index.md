# g2rain-spring-boot-starter 文档

本仓库提供 G2rain 后端服务共享的 Spring Boot Starter 套件。根工程统一版本和构建约束，各子模块通过 Spring Boot 自动配置或聚合依赖向业务服务提供基础设施能力。

## 导航

- [v1.0.6 发布草稿与升级注意事项](releases/v1.0.6.md)：当前完整构建被 tracing-otel 的上下文传播依赖问题阻断，尚不可正式发布。

- [项目元数据](project.yaml)
- [架构总览](architecture/overview.md)
- [模块说明](architecture/modules.md)
- [依赖与兼容性](architecture/dependencies.md)
- [运行流程](architecture/runtime-flows.md)
- [本地工程基线](architecture/local-baseline.md)
- [差异与风险](architecture/deviations.md)
- [公共 API 与扩展点](api/public-api.md)
- [配置参考](api/configuration.md)
- [本地开发](development/local-development.md)
- [代码约定](development/code-conventions.md)
- [测试策略](development/testing.md)
- [完成标准](development/definition-of-done.md)
- [Git 工作流](development/git-workflow.md)
- [发布说明](operations/publishing.md)
- [安全边界](security/security-boundaries.md)
- [需求记录](requirements/README.md)
- [架构决策](decisions/README.md)

## 当前验证

当前版本事实来源为根 `pom.xml` 的 `revision=1.0.6`。2026-10-30 执行 `mvn -B clean verify` 失败：Web Infra 的 51 项测试通过，但 tracing-otel 编译缺少 com.g2rain.common.concurrent 包，后续模块跳过，详见发布草稿。

历史记录：2026-09-06 执行 `mvn test` 成功，12 个 Reactor 项目全部通过，共 101 项测试，0 失败、0 错误、0 跳过。该历史结果不代表 v1.0.6 已通过验证。
