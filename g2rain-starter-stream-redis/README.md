# g2rain-starter-stream-redis

`g2rain-starter-stream-redis` 是一个基于 Redis 的 Spring Cloud Stream Binder Starter。  
它在当前技术栈（Spring Boot 4 + Spring Cloud 2025）下复刻 `spring-cloud-stream-binder-redis` 的核心能力，目标是**补齐能力而非扩展设计**。

## 设计目标

- 对齐经典 Redis Binder 的核心语义（group、partition、retry、error queue）
- 保持与 `spring-cloud-stream-binder-redis-master` 的行为一致性
- 在新版本依赖体系中可稳定启动与装配

## 模块结构

- `com.g2rain.stream.redis.binder.G2rainRedisMessageChannelBinder`
  - Binder 核心实现
  - 负责 producer/consumer 绑定、消息路由、并发消费、重试降级
- `com.g2rain.stream.redis.config.G2rainRedisBinderAutoConfiguration`
  - 自动配置入口
  - 注册 Binder Bean 与健康检查 Bean
- `com.g2rain.stream.redis.config.G2rainRedisBinderProperties`
  - Binder 扩展配置项（header 扩展）

## 核心流程

### 1) Producer 发送流程

1. 绑定输出通道，创建 `SendingHandler`
2. 读取 `groups.<destination>` 中有效 group
3. 按 `destination.group` fan-out 发送
4. 分区场景路由为 `destination.group-<partition>`
5. 根据 `HeaderMode` 执行 embedded/raw 处理

### 2) Consumer 接收流程

1. 绑定输入通道，目标队列为 `destination.group`
2. 分区场景绑定 `destination.group-<instanceIndex>`
3. 并发场景创建多个 `RedisQueueMessageDrivenEndpoint`
4. embedded 模式下提取 headers 并还原消息

### 3) 重试与错误队列

- 当 `maxAttempts > 1` 时启用重试通道
- 重试耗尽后将消息发送到 `ERRORS:<queueName>`

## 关键语义对齐说明

- group 维护：使用 `groups.<destination>` 的 Redis ZSET 管理活跃 group
- requiredGroups：producer 启动时预注册 group
- raw 模式：严格要求 `application/octet-stream` + `byte[]`
- header 合并：`STANDARD_HEADERS + 自定义 headers`

## 配置说明

### Binder 扩展配置

前缀：`spring.cloud.stream.redis.binder`

- `spring.cloud.stream.redis.binder.headers`  
  额外需要嵌入消息的 header 字段列表
- `spring.cloud.stream.redis.binder.no-group-consumer-mode`
  consumer 未配置 `group` 时的语义（默认 `pubsub`）：
  - `pubsub`：使用 Redis PUB/SUB（广播；不落地，晚启动收不到历史消息）
  - `anon-queue`：为每个实例生成匿名 group，走队列模式（可靠广播；会产生临时队列，需要治理）

### 默认 consumer 配置

位于：`META-INF/spring-cloud-stream/redis-binder.properties`

- `spring.cloud.stream.default.consumer.backOffInitialInterval=1000`
- `spring.cloud.stream.default.consumer.backOffMaxInterval=10000`
- `spring.cloud.stream.default.consumer.backOffMultiplier=2.0`
- `spring.cloud.stream.default.consumer.concurrency=1`
- `spring.cloud.stream.default.consumer.maxAttempts=3`

## 自动装配与发现

- `META-INF/spring.binders`
  - 声明 binder 类型 `redis` 对应自动配置类
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  - Boot 4 自动配置导入入口

## 使用建议

- 本 starter 聚焦“与老 Redis Binder 功能对齐”
- 不建议在此模块叠加业务语义
- 业务侧通过标准 Spring Cloud Stream 绑定配置使用即可

## 快速接入示例

下面给出一个最小可运行的 `application.yml` 示例（仅示意 binder 与 binding 的关键字段）：

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379

  cloud:
    stream:
      defaultBinder: redis
      binders:
        redis:
          type: redis

      bindings:
        syncerEventOut-out-0:
          destination: cache.sync
          producer:
            requiredGroups:
              - basis
              - iam
            partitionCount: 3
        syncerEventIn-in-0:
          destination: cache.sync
          group: basis
          consumer:
            concurrency: 2
            maxAttempts: 3
            partitioned: true
            instanceCount: 3
            instanceIndex: 0

    # Binder 扩展参数
    stream:
      redis:
        binder:
          headers:
            - traceId
            - tenantId
          # 不配置 group 时默认广播（pubsub）；如需可靠广播可切换为 anon-queue
          # no-group-consumer-mode: pubsub
```

> 说明 1：`destination` 与 `group` 会映射为 Redis 队列名 `destination.group`；分区消费会追加 `-instanceIndex`。  
> 说明 2：`syncerEventOut-out-0` / `syncerEventIn-in-0` 是示例命名，请替换为你业务服务中实际的 binding 名称。

## 一页式排障清单

- **启动失败：找不到 Redis 连接**
  - 检查 `spring.data.redis.*` 配置是否正确
  - 确认 Redis 可连通（网络、防火墙、密码）

- **消息发送了但消费不到**
  - 检查 producer/consumer 的 `destination` 是否一致
  - 检查 consumer `group` 是否正确（队列名是 `destination.group`）
  - 检查是否误配了分区参数（`instanceCount`/`instanceIndex`）

- **分区场景部分实例收不到**
  - 确认所有实例的 `instanceIndex` 覆盖了 `[0, instanceCount-1]`
  - 确认 producer 端已配置 `partitionCount`

- **出现 raw 模式报错**
  - raw 模式要求 `content-type=application/octet-stream`
  - payload 必须是 `byte[]`
  - 如果想传普通对象，请改用 embedded headers 模式

- **重试后仍失败**
  - 检查 `maxAttempts` 是否大于 1
  - 失败消息会进入 `ERRORS:<queueName>`，可在 Redis 中排查该队列

- **多 binder 场景装配异常**
  - 检查 `spring.cloud.stream.defaultBinder` 是否显式指定为 `redis`
  - 检查 `spring.cloud.stream.binders.redis.type=redis` 是否配置

