package com.g2rain.tracing.otel;

import com.g2rain.common.concurrent.ContextPropagator;
import com.g2rain.common.concurrent.Contexts;
import com.g2rain.common.utils.Collections;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import jakarta.annotation.PostConstruct;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import java.util.Map;

/**
 * <h2>G2rain OpenTelemetry 链路追踪 Starter 的自动配置入口</h2>
 *
 * <p><b>本类在整体 Starter 中的角色：</b></p>
 * <ul>
 *     <li>通过 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 *         被 Spring Boot 4 自动导入，与仓库内其他 {@code g2rain-starter-*} 的写法一致。</li>
 *     <li>启动时向 {@link com.g2rain.common.concurrent.Contexts} 注册传播器：Micrometer
 *         {@link ContextSnapshot} + 整表 MDC，使 {@code ContextExecutors} 起的 VT 能带上
 *         traceId/spanId 及业务 MDC（如 DEBUG_PRINT_LEVEL）。</li>
 *     <li>依赖 {@code feign-micrometer}，在存在 OpenFeign 时由 Spring Cloud 装配
 *         {@code MicrometerObservationCapability}，Feign 出站自动带 W3C {@code traceparent}。</li>
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

    /**
     * 把 Micrometer context-propagation + MDC 接到 {@link Contexts}：
     * 父线程 {@link ContextSnapshotFactory#captureAll(Object...)} 与 MDC 拷贝，
     * 子线程先 {@link ContextSnapshot#setThreadLocals()} 再 set MDC。
     * <p>不改变 HTTP/W3C 传播与 OTLP 导出；仅补齐跨 VT 的同请求 Span/Observation 与日志 MDC。
     */
    @PostConstruct
    public void registerContextPropagator() {
        // captureAll：抓当前线程已注册的 ThreadLocalAccessor（含 OTel/Observation）
        ContextSnapshotFactory factory = ContextSnapshotFactory
            .builder()
            .build();

        Contexts.register(new ContextPropagator() {
            @Override
            public Object capture() {
                Map<String, String> mdc = MDC.getCopyOfContextMap();
                if (Collections.isEmpty(mdc)) {
                    mdc = Map.of();
                } else {
                    mdc = Map.copyOf(mdc);
                }

                return new Captured(factory.captureAll(), mdc);
            }

            @Override
            @SuppressWarnings("resource")
            public AutoCloseable restore(Object snap) {
                Captured captured = (Captured) snap;
                // previous：子线程进场前的 MDC（新 VT 通常为空）
                Map<String, String> previous = MDC.getCopyOfContextMap();
                // 先恢复 Observation/Span，再写 MDC，避免 correlation 按「无 span」清空 trace 字段
                ContextSnapshot.Scope otelScope = captured.otel().setThreadLocals();
                MDC.clear();

                if (!captured.mdc().isEmpty()) {
                    MDC.setContextMap(captured.mdc());
                }

                return () -> {
                    MDC.clear();
                    if (Collections.isNotEmpty(previous)) {
                        MDC.setContextMap(previous);
                    }

                    otelScope.close();
                };
            }
        });
    }

    /**
     * 父线程抓到的 OTel 快照 + MDC 整表。
     */
    private record Captured(ContextSnapshot otel, Map<String, String> mdc) {

    }
}
