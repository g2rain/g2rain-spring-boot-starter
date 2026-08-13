package com.g2rain.web.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HTTP MessageConverter 链测试")
public class HttpMessageConverterChainTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(
            JacksonAutoConfiguration.class,
            HttpMessageConvertersAutoConfiguration.class,
            JsonConverterAutoConfiguration.class
        ))
        // 未显式配置时由 WebInfraPostProcessor 注入 g2rain；此处显式设置便于断言
        .withPropertyValues("spring.http.converters.preferred-json-mapper=g2rain")
        .withUserConfiguration(JsonMapperTestConfig.class);

    @Configuration
    static class JsonMapperTestConfig {

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }
    }

    @Test
    @DisplayName("按 Boot 4 Customizer 拼链并顺序打印 HttpMessageConverter 类名")
    void printServerHttpMessageConverterChain() {
        contextRunner.run(context -> {
            List<HttpMessageConverter<?>> converters = buildServerConverterChain(context);

            System.out.println("=== Server HttpMessageConverter chain (preferred-json-mapper=g2rain) ===");
            for (String className : toSimpleClassNames(converters)) {
                System.out.println(className);
            }

            assertThat(converters).isNotEmpty();
        });
    }

    static List<HttpMessageConverter<?>> buildServerConverterChain(org.springframework.context.ApplicationContext context) {
        HttpMessageConverters.ServerBuilder builder = HttpMessageConverters.forServer().registerDefaults();
        ObjectProvider<ServerHttpMessageConvertersCustomizer> customizers =
            context.getBeanProvider(ServerHttpMessageConvertersCustomizer.class);
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));

        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        builder.build().forEach(converters::add);
        return converters;
    }

    static List<String> toSimpleClassNames(List<HttpMessageConverter<?>> converters) {
        return converters.stream()
            .map(converter -> converter.getClass().getSimpleName())
            .collect(Collectors.toList());
    }
}
