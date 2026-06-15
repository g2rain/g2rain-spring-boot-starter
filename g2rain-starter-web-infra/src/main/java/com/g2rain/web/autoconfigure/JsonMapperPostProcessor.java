package com.g2rain.web.autoconfigure;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;
import java.util.Objects;

/**
 * 在未显式配置时注入 {@code spring.http.converters.preferred-json-mapper=g2rain}。
 * <p>
 * Starter jar 内的 {@code application.yml} 不会合并进业务服务 Environment，需在后处理器阶段补充默认值。
 * </p>
 */
public class JsonMapperPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "g2rainWebInfraDefaults";

    private static final String PREFERRED_JSON_MAPPER = "spring.http.converters.preferred-json-mapper";

    @Override
    public void postProcessEnvironment(@NonNull ConfigurableEnvironment environment, @NonNull SpringApplication application) {
        if (Objects.nonNull(environment.getProperty(PREFERRED_JSON_MAPPER))) {
            return;
        }

        environment.getPropertySources().addLast(new MapPropertySource(
            PROPERTY_SOURCE_NAME, Map.of(PREFERRED_JSON_MAPPER, "g2rain")
        ));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
