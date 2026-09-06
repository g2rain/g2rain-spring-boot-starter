# 依赖与兼容性

根工程继承 Spring Boot Starter Parent 4.0.5，导入 Spring Cloud 2025.1.1，Java release 为 25。内部产物统一使用 `com.g2rain` 组坐标与 `${revision}` 版本。

主要跨仓库依赖包括 `g2rain-common 1.0.7`、`g2rain-mybatis-extension 1.0.4` 和 `g2rain-mybatis-pagination 1.0.4`。MyBatis、JSqlParser、Caffeine、SpringDoc、Redisson 等版本由根 POM 管理。

## 依赖方向

- 聚合模块可以依赖功能 Starter，功能 Starter 不应反向依赖聚合模块。
- 功能 Starter 之间仅在确有运行时组合关系时建立依赖，避免形成隐式“大而全”Starter。
- 外部服务客户端依赖 G2rain 公共模型与身份上下文，但不能把业务服务实现带入 Starter。

升级 Spring Boot、Spring Cloud 或 G2rain 公共组件时，需要完整 Reactor 测试，并重点验证自动配置条件、Jackson、Servlet API、Feign、Stream Binder 与 MyBatis 插件兼容性。
