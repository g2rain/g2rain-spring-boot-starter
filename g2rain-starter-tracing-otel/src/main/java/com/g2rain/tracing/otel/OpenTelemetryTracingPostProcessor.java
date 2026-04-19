package com.g2rain.tracing.otel;

import lombok.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * <h2>G2rain OpenTelemetry 链路追踪：环境后处理器</h2>
 *
 * <p><b>整体功能：</b>在 {@link SpringApplication#run(String...)} 执行过程中、创建并刷新 {@code ApplicationContext}
 * 之前的<b>环境准备阶段</b>（此阶段会回调已注册的 {@link EnvironmentPostProcessor}），
 * 把本 Starter 自带的默认配置从 classpath 上的 YAML 文件加载为 {@link PropertySource}，
 * 并挂到 {@link ConfigurableEnvironment} 的<b>末尾</b>，从而：</p>
 * <ul>
 *     <li>为 Actuator / Micrometer 等提供低优先级的 {@code management.tracing.*}、{@code management.otlp.*}、
 *         {@code management.endpoints.*} 等默认项；</li>
 *     <li>为日志系统提供 {@code logging.pattern.console}、{@code logging.pattern.file} 等默认格式（含 traceId、spanId、
 *         requestId 等 MDC 占位符）。</li>
 * </ul>
 *
 * <p><b>为何必须实现 {@link EnvironmentPostProcessor} 而不能只靠 {@code @AutoConfiguration}：</b>
 * {@code logging.pattern.*} 会在 {@link org.springframework.boot.context.logging.LoggingApplicationListener}
 * 响应环境就绪事件时被消费；该时机早于 {@code ApplicationContext} 的 {@code refresh}，
 * 因此晚于该时机的自动配置类无法可靠地注入日志相关默认项。本类由
 * {@code META-INF/spring.factories} 中的 {@code org.springframework.boot.EnvironmentPostProcessor} 键注册，
 * 由框架在正确阶段回调。</p>
 *
 * <p><b>默认配置存放位置：</b>{@code META-INF/g2rain/tracing-otel-defaults.yaml}。与 Java 代码分离，便于评审与版本 diff。</p>
 *
 * <p><b>与 {@link Ordered}：</b>使用 {@link Ordered#LOWEST_PRECEDENCE}，使本 Starter 的默认层尽量靠后，
 * 保证应用自身的 {@code application.yml}、环境变量、命令行参数等更高优先级来源可以覆盖上述默认值。</p>
 *
 * @author alpha
 * @see OpenTelemetryTracingAutoConfiguration
 * @since 2026/4/13
 */
public class OpenTelemetryTracingPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * 追加到 {@link ConfigurableEnvironment} 中的 {@link PropertySource} 的逻辑名称。
     * <p>用于幂等：若环境中已存在同名 PropertySource，则跳过重复加载，避免多次执行后处理器时重复追加。</p>
     */
    private static final String PROPERTY_SOURCE_NAME = "g2rainTracingOtelDefaults";

    /**
     * 相对于 classpath 根路径的默认 YAML 资源路径（位于本 Starter JAR 的 {@code META-INF/g2rain/} 下）。
     * <p>内容由 {@link YamlPropertySourceLoader} 解析；多文档 YAML 时会得到多个 {@link PropertySource}，均依次 {@code addLast}。</p>
     */
    private static final String DEFAULTS_RESOURCE = "META-INF/g2rain/tracing-otel-defaults.yaml";

    /**
     * Spring Boot 在合并完用户配置数据后调用：将 jar 内默认 YAML 转为属性源并挂到环境末尾。
     *
     * @param environment 可配置的环境，可向其中增删 {@link PropertySource}
     * @param application 当前启动的 Spring 应用实例（本实现未使用，保留以满足接口签名；标注 {@link NonNull} 与 Lombok/空安全一致）
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, @NonNull SpringApplication application) {
        // 已加载过则直接返回，防止同一进程内多次 post-process 导致重复 PropertySource
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }

        // 从当前类加载器链解析 classpath 资源（Starter 打 jar 后资源位于 META-INF/g2rain/...）
        Resource resource = new ClassPathResource(DEFAULTS_RESOURCE);
        if (!resource.exists()) {
            // 打包缺失属于构建/发布错误，快速失败便于发现问题
            throw new IllegalStateException("Missing classpath resource " + DEFAULTS_RESOURCE);
        }

        try {
            // Spring Boot 提供的 YAML → PropertySource 列表（支持多文档 YAML）
            List<PropertySource<?>> loaded = new YamlPropertySourceLoader().load(PROPERTY_SOURCE_NAME, resource);
            // 一律 addLast：整段默认配置处于低优先级，业务配置可覆盖
            for (PropertySource<?> source : loaded) {
                environment.getPropertySources().addLast(source);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to load classpath:" + DEFAULTS_RESOURCE, ex);
        }
    }

    /**
     * 定义本后处理器相对于其他 {@link EnvironmentPostProcessor} 的执行顺序。
     *
     * @return {@link Ordered#LOWEST_PRECEDENCE}，尽量在其他后处理器之后执行，使本 Starter 默认值更靠「底层」
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
