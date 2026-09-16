# 本地开发

## 环境

- JDK 25
- Maven 3.9 或与当前插件兼容的更新版本
- 相关 Starter 的外部基础设施，仅在集成验证时需要

## 命令

```shell
mvn test
mvn clean verify
```

开发单个模块时带上其 Reactor 依赖：

```shell
mvn -pl g2rain-starter-web-infra -am test
```

根 POM 管理公共依赖与插件。版本调整统一修改 `revision`，不要在子模块复制版本。不要提交 `target/`、`.flattened-pom.xml` 或 IDE 配置。
