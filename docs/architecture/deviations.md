# 差异与风险

## 已确认差异

- 中央 G2rain 目录没有本项目适用的正式 Profile 或项目登记，当前仅采用本地基线。
- `mvn test` 成功并执行 101 个测试，但测试仅分布在 Web、Redis、缓存同步与 Redis Stream 模块；身份、追踪、MyBatis 数据隔离、Feign、OpenAPI 和部门主体模块没有实际测试执行。
- JaCoCo 在各模块均提示缺少执行数据。根 POM 的 Surefire `argLine` 覆盖了 JaCoCo 动态写入的代理参数，当前不能声明覆盖率。
- `g2rain.data.isolation` 的源码属性已经包含权限策略服务配置，但手写 `spring-configuration-metadata.json` 尚未列出这些键，存在配置文档漂移。
- 根目录存在被忽略的 `.flattened-pom.xml` 和构建产物；它们不是提交内容。

## 待补验证

- 为未执行测试的 Starter 增加条件装配、配置绑定、用户 Bean 回退和关键失败路径测试。
- 使用真实或容器化环境验证 MyBatis 数据隔离、Redis Binder、远程客户端与链路追踪。
- 修正 Surefire/JaCoCo 参数合并后建立可重复的覆盖率基线。
- 校验 README、配置元数据和源码默认值的一致性。
- 明确并测试外部服务不可用、权限策略解析失败及消息重复/乱序时的行为。
