# G2rain Spring Boot Starter 协作约定

## 项目定位

本仓库是 G2rain 的 Spring Boot Starter 套件，统一管理 Starter 版本、依赖和构建规则，并提供 Web、身份、链路追踪、MyBatis 数据隔离、Redis、缓存同步、Feign、Redis Stream、OpenAPI 与部门主体能力。

## 工作入口

- 开始修改前阅读 `docs/index.md`、`docs/project.yaml` 及任务涉及的专题文档。
- 根 `pom.xml`、模块 POM、自动配置类、配置元数据、测试和工作流是当前实现的事实来源。
- 新增或调整模块、自动配置、配置键、外部服务契约、消息绑定、公共 API 或发布行为时，同步更新 `docs/`。
- 不在未经测试的情况下声明数据库、消息中间件、服务端点或 Spring Boot 版本兼容性。

## 常用验证

```shell
mvn test
mvn clean verify
```

发布命令会产生外部影响，仅在明确授权、版本一致、凭据齐全且发布检查完成后执行：

```shell
mvn -P release clean deploy
```

## 完成标准

- 改动位于正确 Starter，自动配置具备合理的条件与回退机制。
- 公共行为和配置绑定有对应测试，`mvn test` 通过。
- 文档与配置元数据同步，不提交 `target/`、`.flattened-pom.xml`、IDE 文件或凭据。
- 明确说明未验证项、兼容性影响与剩余风险。
