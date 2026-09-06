# 代码约定

- 一个 Starter 聚焦一种基础设施能力；跨模块组合放入专门的聚合 POM。
- 自动配置优先使用 `@ConditionalOnClass`、`@ConditionalOnBean`、`@ConditionalOnMissingBean` 和属性开关，避免抢占业务 Bean。
- 自动配置类必须登记在 `AutoConfiguration.imports`；环境前置处理器才使用 `spring.factories`。
- 属性类、配置元数据和文档保持同名、同默认值、同含义。
- 外部服务契约通过接口隔离，服务发现名称与直连 URL 的优先级必须明确。
- Filter、Interceptor、MyBatis 处理器和消息订阅器必须记录顺序、清理与重复执行语义。
- 日志不得泄露令牌、身份敏感头、密码或未经脱敏的业务数据。
