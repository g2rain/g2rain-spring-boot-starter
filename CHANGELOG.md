# Changelog

## 1.0.6 — 待发布

- 更新 Spring Boot 4.1.2、Spring Cloud 2025.1.4 与相关依赖版本声明。
- Feign 调试日志对主体名称、组织名称的副本解码，不修改原始传输 Header。
- 登录守卫要求主体 ID 为正数；ANONYMOUS 不再无条件放行，升级时需检查公开接口。
- 新增匿名会话相关登录守卫回归测试。
- **发布阻断**：2026-10-30 完整 verify 在 tracing-otel 模块编译失败，缺少 com.g2rain.common.concurrent 包。尚未正式发布。

详细变更、依赖清单与验证记录见 [v1.0.6 发布草稿](docs/releases/v1.0.6.md)。
