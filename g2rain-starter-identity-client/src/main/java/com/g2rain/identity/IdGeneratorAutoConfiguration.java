package com.g2rain.identity;


import com.g2rain.common.id.IdGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

/**
 * @author alpha
 * @since 2025/12/30
 */
@AutoConfiguration
@EnableFeignClients(clients = IdGeneratorClient.class)
@EnableConfigurationProperties(IdGeneratorProperties.class)
public class IdGeneratorAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public IdGenerator idGenerator(IdGeneratorClient idGeneratorClient) {
        return new IdGeneratorImpl(idGeneratorClient);
    }
}
