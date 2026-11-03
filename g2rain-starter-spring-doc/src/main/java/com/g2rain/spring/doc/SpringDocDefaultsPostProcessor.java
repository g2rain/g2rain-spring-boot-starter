package com.g2rain.spring.doc;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * SpringDoc 全局默认配置：以最低优先级挂到环境末尾，各服务配置可覆盖。
 *
 * <p>通过 {@code META-INF/spring.factories} 注册为 {@link EnvironmentPostProcessor}。</p>
 *
 * @author alpha
 * @since 2026/11/3
 */
public class SpringDocDefaultsPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * 属性源名称，用于幂等：已存在时不重复追加。
     */
    private static final String PROPERTY_SOURCE_NAME = "g2rainSpringDocDefaults";

    /**
     * 默认属性：查询 DTO 等对象参数平铺为独立的 query 参数。
     */
    private static final Map<String, Object> DEFAULTS = Map.of(
        "springdoc.default-flat-param-object", true
    );

    /**
     * 在用户配置加载后追加默认属性源。
     *
     * @param environment 可配置的环境
     * @param application 当前启动的应用（未使用）
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, @Nullable SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }

        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULTS));
    }

    /**
     * @return {@link Ordered#LOWEST_PRECEDENCE}，在其他后处理器之后执行
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
