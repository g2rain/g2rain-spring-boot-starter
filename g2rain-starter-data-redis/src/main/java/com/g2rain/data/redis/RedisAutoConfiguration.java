package com.g2rain.data.redis;


import lombok.NonNull;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Objects;

/**
 * <p>
 * {@code RedisAutoConfiguration} 是 Redis 相关 Bean 的自动装配类。
 * 它会根据当前 Spring 环境中可用的 Redis 组件自动创建对应的工具类 Bean。
 * </p>
 * <p>
 * 支持自动装配：
 * <ul>
 *     <li>{@link StringRedisHelper} — 基于 {@link StringRedisTemplate} 的 Redis 操作工具类</li>
 *     <li>{@link GenericRedisHelper} — 基于 {@link RedisTemplate} 的 Redis 通用操作工具类</li>
 *     <li>{@link DistributedLock} — 基于 {@link RedissonClient} 的分布式锁工具类</li>
 * </ul>
 * </p>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * @SpringBootApplication
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 *
 * @Autowired
 * private StringRedisHelper stringRedisHelper;
 *
 * @Autowired
 * private GenericRedisHelper genericRedisHelper;
 *
 * @Autowired
 * private DistributedLock distributedLock;
 * }</pre>
 *
 * @author alpha
 * @since 2025/10/6
 */
@AutoConfiguration
public class RedisAutoConfiguration {

    /**
     * 自动装配 {@link StringRedisHelper}。
     * <p>
     * 当类路径中存在 {@link StringRedisTemplate} 且容器中存在该 Bean，
     * 且当前容器中未存在 {@link StringRedisHelper} 时自动创建。
     * </p>
     *
     * @param provider 已配置的 {@link StringRedisTemplate} Bean
     * @return {@link StringRedisHelper} 实例
     */
    @Bean
    @ConditionalOnClass(StringRedisTemplate.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(StringRedisHelper.class)
    public StringRedisHelper stringRedisHelper(ObjectProvider<@NonNull StringRedisTemplate> provider) {
        StringRedisTemplate template = provider.getIfAvailable();
        if (Objects.isNull(template)) {
            return null; // 不注册 Bean
        }

        return new StringRedisHelper(template);
    }

    /**
     * 自动装配 {@link GenericRedisHelper}。
     * <p>
     * 当类路径中存在 {@link RedisTemplate} 且容器中存在该 Bean，
     * 且当前容器中未存在 {@link GenericRedisHelper} 时自动创建。
     * </p>
     *
     * @param provider 已配置的 {@link RedisTemplate} Bean
     * @return {@link GenericRedisHelper} 实例
     */
    @Bean
    @ConditionalOnClass(RedisTemplate.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(GenericRedisHelper.class)
    public GenericRedisHelper genericRedisHelper(ObjectProvider<@NonNull RedisTemplate<String, Object>> provider) {
        RedisTemplate<String, Object> template = provider.getIfAvailable();
        if (Objects.isNull(template)) {
            return null; // 不注册 Bean
        }

        return new GenericRedisHelper(template);
    }

    /**
     * 自动装配 {@link DistributedLock}。
     * <p>
     * 当类路径中存在 {@link RedissonClient} 且容器中存在该 Bean，
     * 且当前容器中未存在 {@link DistributedLock} 时自动创建。
     * </p>
     *
     * @param provider 已配置的 {@link RedissonClient} Bean
     * @return {@link DistributedLock} 实例
     */
    @Bean
    @ConditionalOnClass(RedissonClient.class)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(DistributedLock.class)
    public DistributedLock distributedLock(ObjectProvider<@NonNull RedissonClient> provider) {
        RedissonClient redissonClient = provider.getIfAvailable();
        if (Objects.isNull(redissonClient)) {
            return null; // 不注册 Bean
        }

        return new DistributedLock(redissonClient);
    }
}
