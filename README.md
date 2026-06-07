# g2rain-spring-boot-starter

[![Maven Central](https://img.shields.io/maven-central/v/com.g2rain/g2rain-spring-boot-starter.svg)](https://search.maven.org/artifact/com.g2rain/g2rain-spring-boot-starter)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java Version](https://img.shields.io/badge/Java-25+-orange.svg)](https://openjdk.java.net/)

> 企业级 Spring Boot Starter 集合，为微服务开发提供统一的基础设施支持。

## 📋 项目简介

g2rain-spring-boot-starter 是本仓库的**聚合父 POM**，统一管理版本与构建配置；实际可引入的 artifact 为多个子模块 Starter（当前 **11** 个）。项目遵循 Spring Boot 自动配置约定，模块职责单一，可按需引入。

**当前版本**与根 `pom.xml` 中 `revision` 一致（当前为 **1.0.1**，发布前请核对仓库内实际版本）。

## ✨ 子模块一览（与 `pom.xml` 中 `<modules>` 一致）

| 模块 | ArtifactId | 说明 | 文档 |
|------|------------|------|------|
| 核心防护 | `g2rain-starter-aegis-core` | 微服务基础能力聚合与统一依赖 | — |
| Web 基础设施 | `g2rain-starter-web-infra` | 过滤器、拦截器、全局异常与访问日志等 | — |
| MyBatis 扩展 | `g2rain-starter-mybatis-extensions` | MyBatis 插件链、分页、机构/部门数据隔离 | [README](g2rain-starter-mybatis-extensions/README.md) |
| Redis | `g2rain-starter-data-redis` | Redis 封装、Redisson 分布式锁等 | — |
| 缓存同步 | `g2rain-starter-cache-sync` | 跨实例缓存事件同步 | [README](g2rain-starter-cache-sync/README.md) |
| 身份客户端 | `g2rain-starter-identity-client` | 分布式身份生成 | — |
| Feign 增强 | `g2rain-starter-feign-plus` | OpenFeign 相关增强 | — |
| 链路追踪 | `g2rain-starter-tracing-otel` | OpenTelemetry 追踪集成 | — |
| Stream Redis | `g2rain-starter-stream-redis` | Spring Cloud Stream + Redis 绑定 | [README](g2rain-starter-stream-redis/README.md) |
| SpringDoc | `g2rain-starter-spring-doc` | OpenAPI 文档公共配置 | — |
| 部门 Principal | `g2rain-starter-department-principal` | 登录态写入部门路径，配合数据隔离 | — |

## 🚀 快速开始

### 环境要求

- **JDK**：25+（与父 POM `maven.compiler.release` 一致）
- **Maven**：3.6+
- **Spring Boot**：4.x（父 POM 继承 `spring-boot-starter-parent`，当前为 **4.0.5**）

### 安装依赖

#### 父 POM（可选，用于统一版本管理）

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.g2rain</groupId>
            <artifactId>g2rain-spring-boot-starter</artifactId>
            <version>1.0.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 按需引入（示例版本请与 Maven Central 或本仓库 `revision` 保持一致）

```xml
<!-- 核心防护（常用作基础依赖） -->
<dependency>
    <groupId>com.g2rain</groupId>
    <artifactId>g2rain-starter-aegis-core</artifactId>
    <version>1.0.1</version>
</dependency>

<!-- Web 基础设施 -->
<dependency>
    <groupId>com.g2rain</groupId>
    <artifactId>g2rain-starter-web-infra</artifactId>
    <version>1.0.1</version>
</dependency>

<!-- MyBatis 扩展（分页等） -->
<dependency>
    <groupId>com.g2rain</groupId>
    <artifactId>g2rain-starter-mybatis-extensions</artifactId>
    <version>1.0.1</version>
</dependency>

<!-- 缓存同步 -->
<dependency>
    <groupId>com.g2rain</groupId>
    <artifactId>g2rain-starter-cache-sync</artifactId>
    <version>1.0.1</version>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>com.g2rain</groupId>
    <artifactId>g2rain-starter-data-redis</artifactId>
    <version>1.0.1</version>
</dependency>

<!-- 部门 Principal 增强（配合数据隔离） -->
<dependency>
    <groupId>com.g2rain</groupId>
    <artifactId>g2rain-starter-department-principal</artifactId>
    <version>1.0.1</version>
</dependency>

<!-- SpringDoc 公共配置 -->
<dependency>
    <groupId>com.g2rain</groupId>
    <artifactId>g2rain-starter-spring-doc</artifactId>
    <version>1.0.1</version>
</dependency>
```

### Maven Central 与 CI

- **Release**：推送标签 `v*.*.*` 触发 [Release 工作流](.github/workflows/release.yml)，使用 `-P release` 签名并发布至 Maven Central（需配置仓库 Secrets：`CENTRAL_PORTAL_*`、`MAVEN_GPG_*`）。
- **Snapshot**：`develop` 分支在 `project.version` 为 `*-SNAPSHOT` 时部署快照（见 [.github/workflows/snapshot.yml](.github/workflows/snapshot.yml)）。

## 📖 使用指南

### Web基础设施配置

```yaml
g2rain:
  web:
    enabled: true
    http-wrapper-filter-order: 100
    principal-context-filter-order: 150
    access-log-filter-order: 200
    login-guard-interceptor-order: 300
    identity-param-injector-order: 400
```

```java
@RestController
public class DemoController {

    @LoginGuard(require = true, anonymous = false)
    @IdentityInject(userIdPropertyName = "userId")
    @GetMapping("/api/demo")
    public Result<String> demo(@RequestParam String param) {
        return Result.success("Hello World");
    }
}
```

### 数据隔离配置

最小示例（仅机构隔离）。完整说明（部门策略、`@DataIsolation` 属性、远程服务配置、排障）见 [g2rain-starter-mybatis-extensions/README.md](g2rain-starter-mybatis-extensions/README.md)。

```yaml
g2rain:
  data:
    isolation:
      enabled: true
  principal:
    department:
      enabled: true   # 部门隔离时需要，向 Principal 写入 deptPath
```

```java
@DataIsolation(
    isolationModule = "order",
    isolationTable = "order",
    userIdColumnName = "owner_user_id",
    deptPathColumnName = "dept_path"
)
@Mapper
public interface OrderMapper {

    @Select("SELECT * FROM `order` WHERE status = #{status}")
    List<Order> findByStatus(@Param("status") String status);
}
```

### 缓存同步配置

```yaml
g2rain:
  syncer:
    enabled: true
    redis:
      # Redis 发布器开关
      publisher-enabled: true
      # Redis 订阅器开关
      subscriber-enabled: true
      # Redis 发布主题
      publisher-topic: g2rain:events
      # Redis 订阅主题列表
      subscriber-topics:
        - g2rain:events
```

### Redis数据处理

```java
    @Autowired
    private GenericRedisHelper redisHelper;

    // Value操作
    redisHelper.set("key1", "value1");
    String value = redisHelper.get("key1", String.class);

    // Hash操作
    redisHelper.hSet("hashKey", "field1", "hValue1");
    String hValue = redisHelper.hGet("hashKey", "field1", String.class);

    // List操作
    redisHelper.lPush("listKey", "listValue1");
    List<String> values = redisHelper.lRange("listKey", 0, -1, String.class);
```

## 🏗️ 项目结构

```
g2rain-spring-boot-starter/
├── g2rain-starter-aegis-core/           # 核心防护
├── g2rain-starter-web-infra/            # Web 基础设施
├── g2rain-starter-mybatis-extensions/   # MyBatis 扩展、分页、数据隔离
├── g2rain-starter-data-redis/           # Redis / Redisson
├── g2rain-starter-cache-sync/           # 缓存同步
├── g2rain-starter-identity-client/      # 身份客户端
├── g2rain-starter-feign-plus/           # Feign 增强
├── g2rain-starter-tracing-otel/         # OpenTelemetry 追踪
├── g2rain-starter-stream-redis/         # Stream + Redis
├── g2rain-starter-spring-doc/           # SpringDoc 公共配置
├── g2rain-starter-department-principal/ # 部门 Principal 增强
├── pom.xml
└── README.md
```

## 📊 模块对比

| 模块 | 主要功能 | 适用场景 |
|------|----------|----------|
| aegis-core | 核心依赖聚合与基础防护 | 微服务底座 |
| web-infra | 过滤器、拦截器、统一异常与日志 | 所有 Web 应用 |
| mybatis-extensions | MyBatis 插件链、分页、机构/部门数据隔离 | 持久层增强 |
| cache-sync | 缓存跨节点同步 | 分布式缓存一致性 |
| data-redis | Redis 工具与分布式锁 | 缓存、锁 |
| identity-client | 分布式 ID / 身份 | 需要唯一标识的业务 |
| feign-plus | Feign 调用增强 | 服务间调用 |
| tracing-otel | 链路追踪 | 可观测性 |
| stream-redis | Stream 消息与 Redis 绑定 | 事件驱动 |
| spring-doc | OpenAPI 文档公共配置 | 需要统一 API 文档的微服务 |
| department-principal | 登录态部门路径增强 | 配合 mybatis-extensions 部门隔离 |

## 🔧 开发指南

### 代码规范

项目遵循以下规范：
- **Google Java代码规范**
- **Spring Boot最佳实践**
- **模块化设计原则**

### 构建命令

```bash
# 编译所有模块
mvn clean compile

# 运行测试
mvn test

# 代码质量检查
mvn checkstyle:check pmd:check spotbugs:check

# 生成代码覆盖率报告
mvn jacoco:report

# 打包所有模块
mvn clean package
```

### 添加新的Starter

1. 在根目录下创建新的starter模块
2. 继承父POM配置
3. 添加必要的依赖
4. 实现自动配置类
5. 添加配置属性类
6. 更新根POM的modules配置

## 🧪 测试指南

### 单元测试

```bash
# 运行特定模块测试
mvn test -pl g2rain-starter-web-infra

# 运行所有测试
mvn test
```

### 集成测试

```bash
# 运行集成测试
mvn verify
```

## 📈 性能优化

### 数据隔离优化
- 使用缓存减少SQL解析开销
- 合理配置隔离规则避免过度拦截

### 缓存同步优化
- 根据业务需求选择合适的消息中间件
- 合理配置消息队列参数

### Redis操作优化
- 使用批量操作减少网络开销
- 合理设置过期时间

## 🔍 故障排查

### 常见问题

**Q: Web基础设施过滤器不生效？**
A: 检查配置`g2rain.web.enabled=true`，确认过滤器顺序配置正确。

**Q: 数据隔离SQL解析失败？**
A: 检查SQL语法是否正确，确保使用了支持的SQL语句类型。

**Q: 缓存同步消息丢失？**
A: 检查消息中间件配置，确认网络连接正常。

### 调试模式

```yaml
logging:
  level:
    com.g2rain: DEBUG
```

## 🤝 贡献指南

我们欢迎所有形式的贡献！

### 贡献流程

1. **Fork** 本仓库
2. **创建特性分支**：`git checkout -b feature/your-feature-name`
3. **提交更改**：`git commit -m "Add some feature"`
4. **推送分支**：`git push origin feature/your-feature-name`
5. **提交Pull Request**

### 代码贡献要求

- 遵循Google Java代码规范
- 添加完整的单元测试
- 更新相关文档
- 确保所有测试通过
- 代码覆盖率不低于80%

### 新增Starter要求

- 提供完整的自动配置
- 包含配置属性类
- 添加spring-configuration-metadata.json
- 提供详细的使用文档
- 包含完整的测试用例

## 📄 许可证

本项目基于 [Apache 2.0许可证](LICENSE) 开源。

## 📞 联系我们

- **Issues**: [GitHub Issues](https://github.com/g2rain/g2rain/issues)
- **讨论**: [GitHub Discussions](https://github.com/g2rain/g2rain/discussions)
- **邮箱**: g2rain_developer@163.com

## 🙏 致谢

感谢所有为这个项目做出贡献的开发者们！

---

⭐ 如果这个项目对您有帮助，请给我们一个Star！
