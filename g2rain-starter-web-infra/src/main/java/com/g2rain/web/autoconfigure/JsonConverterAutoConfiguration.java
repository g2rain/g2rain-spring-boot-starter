package com.g2rain.web.autoconfigure;

import com.g2rain.web.converters.G2rainJacksonHttpMessageConverter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.Order;
import tools.jackson.databind.json.JsonMapper;

/**
 * 在 {@link JsonMapper} 就绪后注册 G2rain JSON {@link ServerHttpMessageConvertersCustomizer}。
 */
@ConditionalOnClass(JsonMapper.class)
@EnableConfigurationProperties(G2rainWebProperties.class)
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnProperty(name = "spring.http.converters.preferred-json-mapper", havingValue = "g2rain")
@ConditionalOnProperty(prefix = "g2rain.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JsonConverterAutoConfiguration {

    @Bean
    @Order(0)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnBean(JsonMapper.class)
    ServerHttpMessageConvertersCustomizer g2rainJacksonServerHttpMessageConvertersCustomizer(JsonMapper jsonMapper, G2rainWebProperties properties) {
        return builder -> builder.withJsonConverter(new G2rainJacksonHttpMessageConverter(jsonMapper, properties.isEnableResultMixinFilter()));
    }

    @Bean
    @Order(0)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnBean(JsonMapper.class)
    ClientHttpMessageConvertersCustomizer g2rainJacksonClientHttpMessageConvertersCustomizer(JsonMapper jsonMapper, G2rainWebProperties properties) {
        return builder -> builder.withJsonConverter(new G2rainJacksonHttpMessageConverter(jsonMapper, properties.isEnableResultMixinFilter()));
    }
}
