# g2rain-spring-boot-starter

[![Maven Central](https://img.shields.io/maven-central/v/com.g2rain/g2rain-spring-boot-starter.svg)](https://search.maven.org/artifact/com.g2rain/g2rain-spring-boot-starter)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java Version](https://img.shields.io/badge/Java-25+-orange.svg)](https://openjdk.java.net/)

## 1. 徽标与状态标识

- 当前版本通过 `Maven Central` 发布
- 当前运行时要求 `Java 25+`
- 当前构建方式以 `Maven` 为准
- 当前开源许可证为 `Apache 2.0`

## 2. 项目简介

`g2rain-spring-boot-starter` 是 G2rain 平台的 Spring Boot Starter 集合仓库，用于统一沉淀微服务接入平台能力时常用的自动配置与基础设施封装。它不是单个 Starter，而是一组按能力拆分的 Starter 模块集合，覆盖 Web 基础设施、Redis、缓存同步、MyBatis 数据隔离、身份接入、Feign 增强、链路追踪、OpenAPI 文档等能力。

## 3. 平台定位

`g2rain-spring-boot-starter` 位于 G2rain 平台公共基础能力层与工程化接入层之间。  
它主要服务于平台核心后端服务、增强组件和新建微服务项目。  
根仓库负责统一版本、统一质量与统一发布链路，真正的接入入口是各个子 Starter 模块。

## 4. 核心能力

- 聚合型基础 Starter：通过 `g2rain-starter-aegis-core` 一次引入常用基础接入能力
- Web 基础设施接入：过滤器、拦截器、异常处理、访问日志、主体上下文透传
- MyBatis 扩展与数据隔离：分页、机构隔离、部门数据权限、SQL 条件增强
- Redis 与缓存同步：Redis Helper、分布式锁、事件同步适配
- 服务调用与身份链路：身份客户端、Feign 增强
- 可观测性与文档：OpenTelemetry 链路追踪、SpringDoc 统一配置
- 统一发布链路：Maven Central / Snapshot 发布、质量插件与签名流程

## 5. 技术栈

- 语言与运行时：`Java 25`
- 基础框架：`Spring Boot 4.0.5`
- 微服务生态：`Spring Cloud 2025.1.1`
- 构建工具：`Maven`
- 关键能力依赖：`g2rain-common`、`MyBatis`、`Spring Data Redis`、`Redisson`、`Spring Cloud Stream`、`SpringDoc`、`Micrometer Tracing OTel`
- 质量工具：`Checkstyle`、`PMD`、`SpotBugs`、`JaCoCo`

## 6. 快速开始

### 环境要求

- `JDK 25`
- `Maven 3.9+`

### 引入父 POM

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.g2rain</groupId>
            <artifactId>g2rain-spring-boot-starter</artifactId>
            <version>1.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 按需引入 Starter

```xml
<dependency>
    <groupId>com.g2rain</groupId>
    <artifactId>g2rain-starter-aegis-core</artifactId>
    <version>1.0.3</version>
</dependency>

<dependency>
    <groupId>com.g2rain</groupId>
    <artifactId>g2rain-starter-mybatis-extensions</artifactId>
    <version>1.0.3</version>
</dependency>
```

### 本地构建

```bash
mvn clean install
```

### 发布说明

- 正式版通过 Git Tag 触发 `release.yml`
- `develop` 分支上的 `-SNAPSHOT` 版本通过 `snapshot.yml` 发布
- Release 流程包含源码包、Javadoc 包和 GPG 签名

## 7. 项目结构

```text
g2rain-spring-boot-starter/
├── g2rain-starter-aegis-core/
├── g2rain-starter-web-infra/
├── g2rain-starter-mybatis-extensions/
├── g2rain-starter-data-redis/
├── g2rain-starter-cache-sync/
├── g2rain-starter-identity-client/
├── g2rain-starter-feign-plus/
├── g2rain-starter-tracing-otel/
├── g2rain-starter-stream-redis/
├── g2rain-starter-spring-doc/
├── g2rain-starter-department-principal/
├── .github/workflows/
└── pom.xml
```

### 核心能力结构说明

#### 1. `g2rain-starter-aegis-core`：聚合型基础接入包

- 这是一个 `pom` 型 Starter，不承载源码实现
- 它通过聚合 `web-infra`、`identity-client`、`tracing-otel`，给微服务提供一套常用基础接入组合
- 适合希望快速完成统一 Web、身份和链路追踪接入的服务

典型用法：

```xml
<dependency>
    <groupId>com.g2rain</groupId>
    <artifactId>g2rain-starter-aegis-core</artifactId>
    <version>1.0.3</version>
</dependency>
```

#### 2. `g2rain-starter-web-infra`：统一 Web 基础设施

- 自动注册请求包装、主体上下文、访问日志、全局异常等过滤器
- 自动装配登录守卫、身份参数注入和统一 JSON Converter
- 通过 `g2rain.web.*` 配置项控制开关与顺序

典型用法：

```yaml
g2rain:
  web:
    enabled: true
    access-log-filter-enabled: true
    login-guard-interceptor-enabled: true
```

```java
@LoginGuard(require = true, anonymous = false)
@IdentityInject(userIdPropertyName = "userId")
@GetMapping("/api/demo")
public Result<String> demo() {
    return Result.success("ok");
}
```

#### 3. `g2rain-starter-mybatis-extensions`：分页与数据隔离

- 提供分页、机构隔离、部门数据权限和 SQL 条件构建能力
- 通过 `@DataIsolation` 标注 Mapper 接口启用隔离
- 可与 `g2rain-department`、`g2rain-basis`、`g2rain-starter-department-principal` 协作完成动态权限解析

典型用法：

```java
@DataIsolation(
    permissionTableName = "order",
    userIdColumnName = "owner_user_id",
    deptPathColumnName = "dept_path"
)
@Mapper
public interface OrderMapper {
    List<Order> selectList(OrderQuery query);
}
```

#### 4. `g2rain-starter-data-redis`：Redis Helper 与分布式锁

- 自动装配 `StringRedisHelper`、`GenericRedisHelper`、`DistributedLock`
- 按容器中已有 `StringRedisTemplate`、`RedisTemplate`、`RedissonClient` 条件化创建 Bean
- 适合微服务快速接入 Redis 常用操作与 Redisson 锁能力

典型用法：

```java
@Autowired
private GenericRedisHelper redisHelper;

@Autowired
private DistributedLock distributedLock;
```

#### 5. `g2rain-starter-cache-sync`：公共事件同步适配

- 把 `g2rain-common.syncer` 抽象适配到 Spring Cloud Stream
- 自动装配 `EventPublisherHub`、`StreamEventSubscriber`、`MessageDispatcher`
- 适合需要把缓存更新或业务事件同步到消息总线的场景

典型用法：

```java
@Autowired
private EventPublisherHub eventPublisherHub;

eventPublisherHub.sendUpdate("output", "ORDER_SERVICE", payload);
```

#### 6. 其他 Starter 的角色边界

- `g2rain-starter-identity-client`：统一身份客户端接入
- `g2rain-starter-feign-plus`：统一 Feign 增强
- `g2rain-starter-tracing-otel`：统一链路追踪接入
- `g2rain-starter-stream-redis`：Redis Binder / Stream 相关能力
- `g2rain-starter-spring-doc`：统一 OpenAPI 文档装配
- `g2rain-starter-department-principal`：把部门路径增强写入主体上下文，供数据隔离能力使用

#### 7. 接入建议与边界

- 如果希望快速构建标准微服务基础接入，优先使用 `g2rain-starter-aegis-core`
- 如果只需要某一项能力，建议按模块单独引入对应 Starter
- `mybatis-extensions` 与 `department-principal` 在数据权限场景下通常需要配合使用
- 根仓库本身不是运行时依赖入口，不建议业务项目只引入父 `pom` 而不引入具体 Starter

## 8. 常用命令

```bash
mvn clean install
mvn test
mvn -pl g2rain-starter-web-infra -am package
```

## 9. 质量与测试

- 当前测试主要分布于 `g2rain-starter-web-infra`、`g2rain-starter-data-redis`、`g2rain-starter-cache-sync`、`g2rain-starter-stream-redis`
- 测试重点覆盖自动配置、过滤器、拦截器、Redis Helper、缓存同步与 Binder 适配
- 根仓库统一启用了 `maven-enforcer-plugin`、`maven-checkstyle-plugin`、`maven-pmd-plugin`、`spotbugs-maven-plugin` 和 `jacoco-maven-plugin`
- 当前仍建议后续继续补强 `identity-client`、`feign-plus`、`tracing-otel` 等模块的独立测试事实说明

## 10. 相关仓库

- `g2rain-common`
- `g2rain-mybatis-extensions`
- `g2rain-iam`
- `g2rain-infra`
- `g2rain-department`

## 11. 使用建议

- 适合作为 G2rain 平台 Java 微服务的统一基础接入层
- 新服务建议先判断是需要聚合接入还是按能力拆分接入
- 如果项目希望遵循平台统一身份、安全、日志、数据权限和缓存同步规范，应优先从这里选取 Starter
- 不建议把该仓库理解为普通依赖清单仓库，它的核心价值在自动配置与接入标准化

## 12. 贡献指南

欢迎通过文档改进、Issue 反馈、测试补充、代码优化、功能增强等形式参与贡献。

建议流程：
1. Fork 本仓库
2. 创建特性分支
3. 提交修改
4. 推送分支
5. 提交 Pull Request

提交前请尽量确保：
- 遵循现有技术栈与代码规范
- 更新相关文档
- 补充必要测试

## 13. 许可证

本项目基于 [Apache 2.0许可证](LICENSE) 开源。

## 14. 联系我们

- **站点**: https://www.g2rain.com/
- **Issues**: [GitHub Issues](https://github.com/g2rain/g2rain/issues)
- **讨论**: [GitHub Discussions](https://github.com/g2rain/g2rain/discussions)
- **邮箱**: g2rain_developer@163.com

## 15. 致谢

感谢所有为这个项目做出贡献的开发者们。

如果这个项目对您有帮助，欢迎 Star 支持。
