# 配置参考

以下是源码确认的主要配置前缀。具体默认值以属性类和 `g2rain-starter-tracing-otel` 的默认 YAML 为准。

| 前缀 | 模块 | 说明 |
| --- | --- | --- |
| `g2rain.web` | web-infra | 总开关、Filter/Interceptor 开关与顺序、异常处理和 Result MixIn |
| `g2rain.id.generator` | identity-client | ID 服务名称、URL、上下文路径及雪花/业务 ID 路径 |
| `g2rain.principal.department` | department-principal | 部门主体服务地址、路径与启用开关 |
| `g2rain.data.isolation` | mybatis-extensions | 数据隔离、组织层级服务和权限策略服务地址/路径。`enabled` 默认 `true`；组织层级默认服务为 `g2rain-basis`，权限策略默认服务为 `g2rain-department`。 |
| `spring.cloud.stream.redis.binder` | stream-redis | 自定义 headers 与无 group 消费模式 |
| `g2rain.springdoc` | spring-doc | OpenAPI 描述与 API 版本 |
| `management.tracing`、`management.otlp`、`logging.pattern` | tracing-otel | 传播、采样、导出与日志关联默认值 |
| `spring.cloud.stream` | cache-sync | 显式 input/output bindings、destination、group 与 binder |

`g2rain.web` 的默认顺序依次为主体作用域 100、全局异常 120、HTTP 包装 150、主体上下文 200、访问日志 300、登录守卫 400、身份参数注入 500。

### 数据隔离远程服务配置

`IsolationProperties` 还定义了以下当前生效的配置键，用于指定组织层级和权限策略服务的直连地址或路径：

| 配置键 | 默认值 | 用途 |
| --- | --- | --- |
| `g2rain.data.isolation.service-name` | `g2rain-basis` | 组织层级服务的服务发现名称 |
| `g2rain.data.isolation.service-url` | 空 | 开发或调试时的组织层级服务直连地址；配置后覆盖服务发现 |
| `g2rain.data.isolation.service-path` | `organ` | 组织层级服务的基础路径 |
| `g2rain.data.isolation.path-business` | `/hierarchy/exists` | 层级关系校验路径 |
| `g2rain.data.isolation.policy-service-name` | `g2rain-department` | 权限策略服务的服务发现名称 |
| `g2rain.data.isolation.policy-service-url` | 空 | 权限策略服务的直连地址 |
| `g2rain.data.isolation.policy-service-path` | `data_permission_meta` | 权限策略服务的基础路径 |
| `g2rain.data.isolation.policy-resolve-path` | `/policy_resolve` | 权限策略解析路径 |

后四项仍未写入手工维护的 `spring-configuration-metadata.json`，因此 IDE 提示可能不完整；源码属性类和本表是当前行为事实来源。

配置元数据只是 IDE 辅助信息，源码属性类是当前行为事实来源。新增或重命名配置键时必须同步元数据、本文档和测试。
