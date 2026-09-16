# 架构总览

项目分为三层：根 POM 负责版本和构建治理；功能 Starter 负责自动配置与公共能力；聚合 Starter 负责向业务应用提供组合依赖。

```text
业务 Spring Boot 服务
    -> g2rain-starter-aegis-core（可选聚合）
        -> web-infra
        -> identity-client
        -> tracing-otel
    -> 其他按需 Starter
        -> MyBatis 数据隔离 / Redis / 缓存同步 / Feign
        -> Redis Stream Binder / OpenAPI / 部门主体
```

各 Starter 的装配边界由类路径、Bean、配置属性与 Spring Boot 自动配置顺序共同决定。跨服务能力通过 Feign/RestClient、Spring Cloud Stream 或 Redis 连接外部系统，因此业务应用仍负责提供端点、凭据、连接和运行环境。
