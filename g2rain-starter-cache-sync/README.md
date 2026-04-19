# g2rain-starter-cache-sync

`g2rain-starter-cache-sync` 用于把 `g2rain-common` 里的 `common.syncer` 能力适配到 Spring Cloud Stream（Spring Boot 4 / Spring Cloud 2025）。

## 核心能力

- **订阅侧（消费）**：`StreamEventSubscriber` 订阅输入 binding，将消息归一化为 raw JSON 后交给 `MessageDispatcher` 分发到各 `AbstractMessageStorage`
- **发布侧（生产）**：基于 `StreamBridge` 创建 `EventPublisherHub`，将事件发布到 outbound binding
- **初始化**：`SyncerInitializer` 在单例初始化后触发 `AbstractMessageStorage#load()`（如果业务实现了）

## 关键约定（非常重要）

### 1) 只会订阅“输入 binding”

`StreamEventSubscriber` 的订阅名单来源为：

- **优先**：`spring.cloud.stream.input-bindings`（逗号分隔）
- **兜底**：当未配置 `input-bindings` 时，仅订阅命名符合函数式约定的 `*-in-0`

> 这意味着：如果你既不配置 `input-bindings`，binding 名又不是 `*-in-0`，看起来就会“没有消费”。  
> 推荐做法：**显式配置 `spring.cloud.stream.input-bindings`**。

### 2) 不会误订阅 output

本 starter 不会因为你配置了 `output-bindings` 就去订阅 output channel（避免生产者自消费）。

## 最小配置示例

### 作为消费者（订阅并分发）

```yaml
spring:
  cloud:
    stream:
      default-binder: redis
      input-bindings: input
      bindings:
        input:
          destination: cache.sync
          # group: gateway   # 配不配 group 的语义由 binder 决定（见 g2rain-starter-stream-redis）
```

### 作为生产者（发布事件）

```yaml
spring:
  cloud:
    stream:
      default-binder: redis
      output-bindings: output
      bindings:
        output:
          destination: cache.sync
```

## 常见排障

- **能发但不消费**
  - 检查是否配置了 `spring.cloud.stream.input-bindings`
  - 检查 input binding 的 `destination` 是否与生产者一致
- **消费到了但不更新缓存**
  - 检查消息体的 `dataSource` 是否与 `AbstractMessageStorage#dataSource()` 返回值一致

