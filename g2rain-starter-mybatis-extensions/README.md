# g2rain-starter-mybatis-extensions

`g2rain-starter-mybatis-extensions` 在 Spring Boot 应用中集成 **MyBatis 分页** 与 **数据隔离** 能力，基于统一的 `ExecutorCompositeInterceptor` / `StatementHandlerCompositeInterceptor` 插件链装配。

## 核心能力

| 能力 | 说明 |
|------|------|
| 分页 | 通过 `PageContext` 触发 SQL 自动分页与 count 统计 |
| 机构（租户）隔离 | 对 SELECT / UPDATE / DELETE 自动追加 `organ_id = 当前机构` 条件 |
| 部门数据权限 | 配置 `isolationModule` + `isolationTable` 后，向 `g2rain-department` 解析策略，按部门路径 / 用户 / Other 规则过滤 |
| 机构层级校验 | 向 `g2rain-basis` 校验目标机构是否在当前登录机构可访问范围内 |

## 依赖引入

```xml
<dependency>
    <groupId>com.g2rain</groupId>
    <artifactId>g2rain-starter-mybatis-extensions</artifactId>
    <version>1.0.1</version>
</dependency>
```

### 配套 Starter（按需）

| Starter | 作用 |
|---------|------|
| `g2rain-starter-web-infra` | 提供 `PrincipalContextHolder`，隔离逻辑依赖当前登录机构 / 用户 / 部门路径 |
| `g2rain-starter-department-principal` | 登录态构建时从 `g2rain-department` 拉取 `deptPath` 写入 Principal，**部门隔离必需** |
| `spring-cloud-starter-openfeign`（可选） | 存在 Feign 时优先使用 Feign 调用远程服务；否则回退 RestClient HTTP Interface |

## 分页

引入本 Starter 后自动注册 `PaginationQueryProcessor`（插件链顺序 `20000`），使用方式与 `g2rain-mybatis-pagination` 一致：

```java
import com.g2rain.mybatis.pagination.PageContext;
import com.g2rain.mybatis.pagination.model.Page;

Page<Order> page = PageContext.of(1, 10, "create_time desc", () -> {
    orderMapper.selectByCondition(query);
});

long total = page.getTotal();
List<Order> records = page.getResult();
```

分页细节（`PageContext` 重载、`PagingEscape`、虚拟线程等）见 [g2rain-mybatis-extensions 文档](../../g2rain-mybatis-extensions/README.md)。

## 数据隔离

### 生效范围

仅对 **租户类型机构**（`OrganType.isTenant`）的前台请求生效；以下场景**不拦截**：

- 后端系统调用（`PrincipalContextHolder.isBackEnd()`）
- 运营公司账号（`PrincipalContextHolder.isAdminCompany()`）

### 拦截阶段

| SQL 类型 | 处理器 | 行为 |
|----------|--------|------|
| SELECT | `IsolationQueryProcessor` | 校验机构范围 → 追加 `organ_id` 条件 → 可选追加部门权限条件 |
| UPDATE / DELETE | `IsolationConstraintProcessor` | 校验机构范围 → 在 WHERE 中追加 `organ_id` 与写权限条件 |
| INSERT | 当前未装配（`IsolationInsertProcessor` 已废弃） | — |

### 配置项

前缀：`g2rain.data.isolation`（默认 `enabled: true`）

```yaml
g2rain:
  data:
    isolation:
      enabled: true
      # 机构层级校验（g2rain-basis）
      service-name: g2rain-basis
      service-url:                    # 开发直连，非空时覆盖 service-name
      service-path: organ
      path-business: /hierarchy/exists
      # 部门权限策略（g2rain-department）
      policy-service-name: g2rain-department
      policy-service-url:
      policy-service-path: data_permission_meta
      policy-resolve-path: /policy_resolve
```

远程客户端：类路径存在 OpenFeign 时优先 Feign；否则使用带负载均衡的 RestClient。

### Mapper 标注

#### 仅机构隔离

不配置 `isolationModule` / `isolationTable` 时，只做机构字段过滤，不调用部门策略服务：

```java
@DataIsolation
@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE name = #{name}")
    List<User> findByName(@Param("name") String name);
}
```

#### 机构 + 部门动态权限

`isolationModule` / `isolationTable` 须与 `g2rain-department` 中 `data_permission_model.module_code` / `table_name` 一致：

```java
@DataIsolation(
    isolationModule = "order",
    isolationTable = "order",
    organIdColumnName = "organ_id",
    organIdPropertyName = "organId",
    userIdColumnName = "owner_user_id",
    deptPathColumnName = "dept_path"
)
@Mapper
public interface OrderMapper {

    List<Order> selectList(OrderQuery query);
}
```

#### 注解属性

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `isolationModule` | `""` | 权限模型模块编码；为空则不查动态策略 |
| `isolationTable` | `""` | 权限模型表名；为空则不查动态策略 |
| `organIdColumnName` | `organ_id` | 机构列名 |
| `organIdPropertyName` | `organId` | 实体机构属性名（INSERT 场景预留） |
| `userIdColumnName` | `""` | 用户列名；非空时策略条件允许按当前用户过滤 |
| `deptPathColumnName` | `""` | 部门路径列名；非空且策略允许本组读取/写入时，按 `dept_path LIKE '{deptPath}%'` 过滤 |

#### 方法级豁免

```java
@DataIsolation(isolationModule = "order", isolationTable = "order")
@Mapper
public interface OrderMapper {

    @IgnoreIsolation
    List<Order> selectAllForAdmin();
}
```

### 权限条件构建规则（读）

当 `isolationModule` + `isolationTable` 均已配置时，SELECT 额外条件由 `DataPermissionConditionBuilder` 生成，各分支以 **OR** 组合：

1. **用户维度**：配置了 `userIdColumnName` 且 Principal 有 `userId` → `{userIdColumn} = 当前用户`
2. **部门维度**：策略 `groupRead=true` 且配置了 `deptPathColumnName`、Principal 有 `deptPath` → 对逗号分隔的每个部门路径生成 `dept_path LIKE 'path%'`
3. **Other 规则**：策略 `otherRead=true` 且 `otherPermRule` 非空 → 解析为 SQL 表达式追加

UPDATE / DELETE 使用对应的 `groupWrite` / `otherWrite` 与 `buildWriteCondition`。

策略解析请求携带：`organId`、`userId`、`deptPath`（来自 Principal）、`moduleCode`、`tableName`。

### 部门路径前置条件

部门隔离依赖 Principal 中的 `deptPath`。请同时引入：

```xml
<dependency>
    <groupId>com.g2rain</groupId>
    <artifactId>g2rain-starter-department-principal</artifactId>
</dependency>
```

```yaml
g2rain:
  principal:
    department:
      enabled: true
      service-name: g2rain-department
      service-path: department_user_relation
      path-business: /principal_enrichment
```

若 `userId` 或 `deptPath` 缺失，策略解析返回 `null`，此时仅保留机构隔离，不追加部门权限条件。

## 自动装配说明

- 自动配置类：`IsolationAutoConfiguration`
- 生效条件：类路径存在 `SqlSessionFactory`
- 数据隔离子配置：`g2rain.data.isolation.enabled=true`（默认开启）
- 用户自定义 Bean 优先：`DataScopeExaminer`、`DataPermissionPolicyResolver` 等可通过 `@Bean` 覆盖

## 常见排障

| 现象 | 排查方向 |
|------|----------|
| 隔离完全不生效 | 是否租户前台请求；Mapper 是否标注 `@DataIsolation`；`g2rain.data.isolation.enabled` 是否为 `true` |
| 报 `isolation.50001` 租户不存在 | `PrincipalContextHolder.getOrganId()` 为空，检查 `web-infra` 过滤器 / 登录态 |
| 报 `isolation.50002` 租户不在范围 | 当前机构无权访问目标机构，检查 `g2rain-basis` 层级关系接口 |
| 只有机构隔离、没有部门过滤 | 是否配置 `isolationModule`/`isolationTable`；是否引入 `department-principal`；Principal 是否有 `deptPath` |
| 部门策略不更新 | 策略结果有本地 Caffeine 缓存，变更后需按 `PolicyInvalidationLevel` 失效或重启 |
| SELECT 报 SQL 解析失败 | 暂不支持复杂 SELECT（如子查询 FROM、UNION）；简化 SQL 或对该方法使用 `@IgnoreIsolation` |
| 分页与隔离冲突 | 框架内部 count / 权限 SQL 通过 `PagingEscape` 逃逸分页上下文，一般无需手动处理 |

## 调试

```yaml
logging:
  level:
    com.g2rain.data.isolation: DEBUG
```
