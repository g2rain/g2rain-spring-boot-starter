package com.g2rain.tracing.otel;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * <h2>G2rain OpenTelemetry 链路追踪 Starter 的自动配置入口</h2>
 *
 * <p><b>本类在整体 Starter 中的角色：</b></p>
 * <ul>
 *     <li>通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 *         被 Spring Boot 4 自动导入，与仓库内其他 {@code g2rain-starter-*} 的写法一致。</li>
 *     <li>当前仅作为「自动配置锚点」：占位、便于后续在本类中注册 Bean（例如定制 Propagator、与业务协作的扩展点等）。</li>
 *     <li><b>不负责</b>向 {@link org.springframework.core.env.Environment} 写入
 *         {@code management.*}、{@code logging.pattern.*} 等默认值——那些必须在容器刷新<b>之前</b>进入环境，
 *         由 {@link OpenTelemetryTracingPostProcessor} 在环境准备阶段完成（见该类说明）。</li>
 * </ul>
 *
 * <p><b>{@link ConditionalOnClass} 的含义：</b>仅当 classpath 上存在 OpenTelemetry API 核心类型
 * {@code io.opentelemetry.api.OpenTelemetry} 时才启用本自动配置类，避免在排除 OTel 依赖等异常装配下
 * 仍去解析与 OTel 相关的配置层次；与 Spring Boot 自带 OTel 相关自动配置的条件风格一致。</p>
 *
 * <p><b>与 {@code spring.factories} 的关系：</b>{@link OpenTelemetryTracingPostProcessor} 通过
 * {@code META-INF/spring.factories} 注册为 {@link org.springframework.boot.EnvironmentPostProcessor}，
 * 与本类的导入机制相互独立：前者管「环境准备阶段」，后者管「容器内的自动配置」。</p>
 *
 * @author alpha
 * @see OpenTelemetryTracingPostProcessor
 * @since 2026/4/13
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.opentelemetry.api.OpenTelemetry")
public class OpenTelemetryTracingAutoConfiguration {

}
