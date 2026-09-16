# 公共 API 与扩展点

## Web

公开入口包括 `HttpRequestWrapper`、`HttpResponseWrapper`、`G2rainWebProperties`，以及 `@LoginGuard`、`@IdentityInject`。Filter、Interceptor、异常处理器和 JSON Converter 多为自动配置基础设施，替换时应按 Bean 名称和条件装配验证。

## Redis 与事件

`DistributedLock`、`GenericRedisHelper`、`StringRedisHelper` 提供 Redis 使用入口。缓存同步公开事件发布/订阅适配器；Redis Stream 模块公开 Binder 与其属性类。

## 身份与远程调用

`IdGeneratorClient` 提供 ID 生成契约。`DepartmentPrincipalClient` 提供部门主体解析契约。Feign Plus 提供 Contract、请求拦截器和 Decoder，可由同名用户 Bean 替换。

## 数据隔离

主要扩展契约包括 `DataPermissionPolicyClient`、`DataPermissionPolicyResolver`、`DataScopeExaminer` 和 `OrganHierarchyClient`；声明式入口为 `@DataIsolation` 与 `@IgnoreIsolation`。隔离元数据、策略范围和处理器属于公共兼容面，变更必须覆盖 SQL 与授权语义测试。

## OpenAPI 与追踪

SpringDoc 自动配置提供 `OpenAPI` 和 `PropertyCustomizer` Bean。Tracing 模块通过环境后处理器加载低优先级默认配置；业务 `application` 配置可以覆盖这些默认值。
