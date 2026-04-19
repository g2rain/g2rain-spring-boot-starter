package com.g2rain.stream.redis.config;

import com.g2rain.stream.redis.binder.G2rainRedisMessageChannelBinder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.health.DataRedisHealthIndicator;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.codec.Codec;
import org.springframework.messaging.MessageChannel;

/**
 * Redis Binder 自动配置。
 * <p>
 * 负责装配两类基础能力：
 * </p>
 * <ul>
 *     <li>Binder 主体：{@link G2rainRedisMessageChannelBinder}</li>
 *     <li>健康检查：{@code binderHealthIndicator}</li>
 * </ul>
 *
 * <p>
 * 为兼容 Spring Boot 4，本配置显式导入 {@link DataRedisAutoConfiguration}，
 * 并通过 {@code @ConditionalOnMissingBean(G2rainRedisMessageChannelBinder.class)} 避免多 Binder 场景被错误挡掉。
 * </p>
 *
 * @author alpha
 * @since 2026/4/13
 */
@AutoConfiguration
@ConditionalOnClass(Binder.class)
@EnableConfigurationProperties(G2rainRedisBinderProperties.class)
@Import(DataRedisAutoConfiguration.class)
@PropertySource("classpath:/META-INF/spring-cloud-stream/redis-binder.properties")
public class G2rainRedisBinderAutoConfiguration {

    /**
     * 注册 Redis Binder Bean。
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @param properties             Binder 自定义配置
     * @param codecProvider          可选 Codec 提供器
     * @return Redis Binder
     */
    @Bean
    @ConditionalOnMissingBean(G2rainRedisMessageChannelBinder.class)
    public Binder<MessageChannel, ConsumerProperties, ProducerProperties> redisMessageChannelBinder(
        RedisConnectionFactory redisConnectionFactory, G2rainRedisBinderProperties properties,
        ObjectProvider<Codec> codecProvider) {
        return new G2rainRedisMessageChannelBinder(
            redisConnectionFactory,
            codecProvider.getIfAvailable(),
            properties.getNoGroupConsumerMode(),
            properties.getHeaders()
        );
    }

    /**
     * 注册 Binder 级 Redis 健康检查。
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return 健康检查指示器
     */
    @Bean
    @ConditionalOnMissingBean(name = "binderHealthIndicator")
    public DataRedisHealthIndicator binderHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        return new DataRedisHealthIndicator(redisConnectionFactory);
    }
}
