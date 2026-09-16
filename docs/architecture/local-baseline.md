# 本地工程基线

中央 G2rain 架构目录当前没有适用于 Spring Boot Starter 套件的正式 Profile，也没有本项目登记项。本仓库暂采用本地基线 `spring-boot-starter-suite 1.0.0-local`；这描述当前仓库事实，不代表中央标准已经发布或采纳。

## 基线约束

- Maven 多模块工程，Java 25、Spring Boot 4.0.5、Spring Cloud 2025.1.1。
- 根 POM 统一版本、依赖版本、编译、测试、静态检查、覆盖率与发布插件。
- 每个功能 Starter 独立成模块；`g2rain-starter-aegis-core` 仅聚合核心依赖。
- 自动配置通过 `AutoConfiguration.imports` 注册，环境前置处理器通过 `spring.factories` 注册。
- 用户可替换的基础设施 Bean 应使用条件装配，避免无条件覆盖业务配置。
- 发布产物包含源码、Javadoc 与 GPG 签名，并提交 Maven Central。

未来若中央目录新增正式 Profile，应评审差异、固定 Profile 版本与基线引用后再登记采用。
