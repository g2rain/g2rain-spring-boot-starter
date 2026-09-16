# 模块说明

| 模块 | 类型 | 职责 |
| --- | --- | --- |
| `g2rain-starter-aegis-core` | POM | 聚合 Web、身份客户端与 OpenTelemetry 链路追踪 |
| `g2rain-starter-web-infra` | JAR | Web Filter、身份上下文、登录守卫、参数注入、JSON 转换与统一异常处理 |
| `g2rain-starter-mybatis-extensions` | JAR | MyBatis 分页集成和基于策略/组织范围的行级数据隔离 |
| `g2rain-starter-data-redis` | JAR | Redis 操作助手与 Redisson 分布式锁 |
| `g2rain-starter-cache-sync` | JAR | 将 common syncer 事件适配到显式 Spring Cloud Stream bindings |
| `g2rain-starter-identity-client` | JAR | 调用基础设施服务生成雪花 ID 与业务 ID |
| `g2rain-starter-feign-plus` | JAR | GET 参数、身份头透传、统一结果解码与异常转换 |
| `g2rain-starter-tracing-otel` | JAR | OpenTelemetry 低优先级默认配置、传播和日志关联 |
| `g2rain-starter-stream-redis` | JAR | Redis-backed Spring Cloud Stream Binder |
| `g2rain-starter-spring-doc` | JAR | OpenAPI 基础信息与隐藏字段定制 |
| `g2rain-starter-department-principal` | JAR | 部门主体信息远程解析与上下文增强 |

新增模块时必须同步根 POM 的 `modules`、依赖管理、本文档和发布检查。
