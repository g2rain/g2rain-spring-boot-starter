# 运行流程

## Web 请求

`web-infra` 按配置注册请求作用域、异常、包装、身份上下文和访问日志 Filter，以及登录守卫与身份参数注入拦截器。过滤器顺序由 `g2rain.web.*-order` 控制，调整时需验证上下文创建、异常捕获与日志读取顺序。

## 远程身份与部门主体

`identity-client` 调用 `g2rain-infra` 的 ID 接口。`department-principal` 调用 `g2rain-department` 丰富主体信息。两者支持服务发现名称或直连 URL，实际网络、鉴权和容错由应用环境负责。

## 数据隔离

`mybatis-extensions` 解析 Mapper 元数据和数据权限策略，通过 MyBatis 处理器修改查询、更新和插入行为；组织层级与权限策略可通过远程客户端解析并缓存。它属于安全控制路径，失败策略和绕过注解必须审慎评审。

## 事件与 Redis

`cache-sync` 只处理应用显式声明或可安全推断的 Spring Cloud Stream 输入/输出 binding。`stream-redis` 提供 Redis Binder；无 consumer group 时的模式由 binder 配置控制。`data-redis` 提供 Redis 模板助手与分布式锁。

## 追踪与文档

`tracing-otel` 以低优先级默认项启用 W3C 传播、requestId baggage 和日志关联，默认关闭 OTLP metrics/tracing 导出。`spring-doc` 根据应用名生成 OpenAPI Info，并隐藏标记为 `@Schema(hidden=true)` 的属性。
